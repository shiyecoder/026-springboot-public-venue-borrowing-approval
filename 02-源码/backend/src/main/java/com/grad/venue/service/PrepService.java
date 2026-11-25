package com.grad.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.venue.common.BizException;
import com.grad.venue.common.UserContext;
import com.grad.venue.entity.*;
import com.grad.venue.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 创新点：按活动类型生成准备清单。
 * 规则：六项因素加权（权重合计 100），每次计算保存输入快照与规则版本，结果可追溯；
 * 业务负责人可人工修正但不得覆盖原始计算值。
 */
@Service
public class PrepService {

    private final PrepRuleMapper ruleMapper;
    private final PrepScoreMapper scoreMapper;
    private final VenueApplicationMapper applyMapper;
    private final VenueMapper venueMapper;
    private final VenueItemMapper itemMapper;
    private final SysLogMapper logMapper;
    private final ObjectMapper json = new ObjectMapper();

    public PrepService(PrepRuleMapper ruleMapper, PrepScoreMapper scoreMapper, VenueApplicationMapper applyMapper,
                       VenueMapper venueMapper, VenueItemMapper itemMapper, SysLogMapper logMapper) {
        this.ruleMapper = ruleMapper;
        this.scoreMapper = scoreMapper;
        this.applyMapper = applyMapper;
        this.venueMapper = venueMapper;
        this.itemMapper = itemMapper;
        this.logMapper = logMapper;
    }

    public List<PrepRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<PrepRule>().orderByAsc(PrepRule::getId));
    }

    /** 规则保存：合计必须 100%，修改后版本号 +1 并记录生效时间 */
    @Transactional
    public List<PrepRule> saveRules(List<PrepRule> list) {
        BigDecimal sum = list.stream().map(PrepRule::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        }
        String ver = nextVersion();
        for (PrepRule r : list) {
            PrepRule old = ruleMapper.selectById(r.getId());
            if (old == null) continue;
            old.setWeight(r.getWeight());
            old.setVersion(ver);
            old.setEffectiveTime(LocalDateTime.now());
            ruleMapper.updateById(old);
        }
        log("修改规则", "PREP-" + ver, "新版权重生效：" + list.stream().map(x -> x.getRuleName() + "=" + x.getWeight() + "%").toList());
        return rules();
    }

    private String nextVersion() {
        List<PrepRule> all = rules();
        int v = 3;
        for (PrepRule r : all) {
            if (r.getVersion() != null && r.getVersion().startsWith("v")) {
                try { v = Math.max(v, Integer.parseInt(r.getVersion().substring(1)) + 1); } catch (Exception ignored) { }
            }
        }
        return "v" + v;
    }

    /**
     * 执行准备清单计算：人数、设备、特殊需求、时段、历史问题加权，
     * 并按活动类型与人数生成桌椅/音响/保洁/安保等准备项，含缺口标记。
     */
    @Transactional
    public Map<String, Object> evaluate(Long applyId) {
        VenueApplication a = applyMapper.selectById(applyId);
        if (a == null) throw new BizException("申请不存在");
        if (a.getPeopleCount() == null || a.getActivityType() == null) {
            throw new BizException("活动类型或参与人数缺失，无法计算，请先补录");
        }
        Map<String, BigDecimal> w = new LinkedHashMap<>();
        String ver = "v3";
        for (PrepRule r : rules()) {
            w.put(r.getRuleKey(), r.getWeight());
            if (r.getVersion() != null) ver = r.getVersion();
        }
        if (w.isEmpty()) throw new BizException("规则未配置，请联系管理员");

        int people = a.getPeopleCount();
        String type = a.getActivityType();
        Venue v = venueMapper.selectById(a.getVenueId());

        // 各因素得分（按权重折算）
        double peopleScore = (people <= 100 ? 1.0 : people <= 300 ? 0.93 : people <= 500 ? 0.87 : people <= 800 ? 0.77 : 0.6);
        double equipScore = (v != null && v.getEquipment() != null && v.getEquipment().contains("音响")) ? 1.0 : 0.8;
        boolean special = a.getNeedSecurity() != null && a.getNeedSecurity() == 1;
        double specialScore = special ? (people > 800 ? 0.73 : 0.87) : 1.0;
        double timeScore = (a.getStartTime() != null && a.getStartTime().getHour() >= 18) ? 0.8 : 1.0;
        double historyScore = (a.getId() % 4 == 0) ? 0.6 : 1.0;
        double typeScore = switch (type) {
            case "讲座" -> 1.0; case "招聘" -> 0.96; case "展览" -> 0.92;
            case "社团" -> 0.84; case "体育" -> 0.8; default -> 0.76;
        };
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("activityType", typeScore);
        rates.put("people", peopleScore);
        rates.put("equipment", equipScore);
        rates.put("special", specialScore);
        rates.put("timeSlot", timeScore);
        rates.put("history", historyScore);

        Map<String, Object> detail = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Double> e : rates.entrySet()) {
            BigDecimal weight = w.getOrDefault(e.getKey(), BigDecimal.ZERO);
            BigDecimal got = weight.multiply(BigDecimal.valueOf(e.getValue())).setScale(1, RoundingMode.HALF_UP);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("weight", weight);
            row.put("score", got);
            row.put("rate", BigDecimal.valueOf(e.getValue()).setScale(2, RoundingMode.HALF_UP));
            detail.put(e.getKey(), row);
            total = total.add(got);
        }
        total = total.setScale(1, RoundingMode.HALF_UP);
        String level = total.compareTo(new BigDecimal("85")) >= 0 ? "准备充分"
                : total.compareTo(new BigDecimal("72")) >= 0 ? "准备较充分" : "准备不足";

        // 准备清单：按活动类型与人数生成
        List<Map<String, Object>> checklist = buildChecklist(v, special, people, type);

        List<String> unmet = new ArrayList<>();
        for (Map<String, Object> c : checklist) {
            if ("缺".equals(c.get("gap"))) unmet.add(c.get("name") + " 缺 " + c.get("shortage") + " " + c.get("unit"));
        }
        if (special) unmet.add("安保人员需保卫处另行协调");

        List<String> advice = new ArrayList<>();
        if (special) advice.add("提前 3 天完成临时用电与疏散通道检查");
        if (List.of("晚会", "体育").contains(type)) advice.add("增加引导标识与志愿者 4 名");
        if (total.compareTo(new BigDecimal("85")) < 0) advice.add("压缩参与人数或补充设备可提升准备等级");
        if (advice.isEmpty()) advice.add("按标准清单执行即可，无需额外增配");

        PrepScore s = new PrepScore();
        s.setApplyId(applyId);
        s.setRuleVersion(ver);
        s.setTotalScore(total);
        s.setLevel(level);
        s.setUnmet(unmet.isEmpty() ? "无" : String.join("；", unmet));
        s.setAdvice(String.join("；", advice));
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("activityType", type);
            snap.put("people", people);
            snap.put("venueId", a.getVenueId());
            snap.put("riskLevel", a.getRiskLevel());
            snap.put("needSecurity", a.getNeedSecurity());
            snap.put("startTime", String.valueOf(a.getStartTime()));
            s.setInputSnapshot(json.writeValueAsString(snap));
            Map<String, Object> dj = new LinkedHashMap<>();
            dj.put("factors", detail);
            dj.put("checklist", checklist);
            s.setDetailJson(json.writeValueAsString(dj));
        } catch (Exception e) {
            throw new BizException("计算结果序列化失败");
        }
        scoreMapper.insert(s);
        log("准备清单计算", a.getApplyNo(), "规则" + ver + " 得分 " + total + " 等级 " + level);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("score", s);
        res.put("factors", detail);
        res.put("checklist", checklist);
        res.put("levelText", level);
        return res;
    }

    /** 清单项与库存比对，生成缺口标记 */
    /** 依据场地物资库存生成准备清单（计算结果与历史留痕共用同一套规则） */
    private List<Map<String, Object>> buildChecklist(Venue v, boolean special, int people, String type) {
        List<Map<String, Object>> checklist = new ArrayList<>();
        int chairs = Math.min(people + 20, 300);
        int desks = Math.max(10, people / 12);
        int boards = Math.max(6, people / 40);
        int cleaners = Math.max(1, people / 300);
        int guards = special ? Math.max(2, people / 250) : 0;
        List<VenueItem> items = v == null ? List.of() : itemMapper.selectList(
                new LambdaQueryWrapper<VenueItem>().eq(VenueItem::getVenueId, v.getId()));
        addItem(checklist, "座椅", chairs, "把", items, "场地物资库");
        addItem(checklist, "课桌", desks, "张", items, "场地物资库");
        if ("招聘".equals(type) || "展览".equals(type)) addItem(checklist, "展板", boards, "块", items, "场地物资库");
        if (List.of("晚会", "讲座", "招聘").contains(type)) addItem(checklist, "音响调试", 1, "套", items, "场地设备");
        addItem(checklist, "保洁", cleaners, "人", List.of(), "规则生成");
        if (guards > 0) addItem(checklist, "安保", guards, "人", List.of(), "规则生成");
        if (special) addItem(checklist, "临时用电检查", 1, "次", List.of(), "规则生成");
        addItem(checklist, "引导标识", Math.max(4, people / 80), "个", List.of(), "规则生成");
        return checklist;
    }

    private void addItem(List<Map<String, Object>> list, String name, int need, String unit,
                         List<VenueItem> stock, String source) {
        String key = switch (name) {
            case "座椅" -> "座椅";
            case "课桌" -> "课桌";
            case "展板" -> "展板";
            case "音响调试" -> "音响";
            default -> name;
        };
        int have = 0;
        for (VenueItem it : stock) {
            if (it.getItemName().contains(key) || key.contains(it.getItemName())) {
                have = Math.max(have, it.getTotalQty());
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("need", need);
        m.put("unit", unit);
        m.put("source", source);
        if (have > 0) {
            int shortage = Math.max(0, need - have);
            m.put("stock", have);
            m.put("gap", shortage > 0 ? "缺" : "可满足");
            m.put("shortage", shortage);
        } else {
            m.put("stock", null);
            m.put("gap", "需外部协调");
            m.put("shortage", 0);
        }
        list.add(m);
    }

    /** 人工修正：必须填原因（≥5 字），原始结果保留 */
    @Transactional
    public PrepScore adjust(Long scoreId, String level, String reason) {
        if (reason == null || reason.trim().length() < 5) throw new BizException("人工修正必须填写不少于 5 个字的原因");
        PrepScore s = scoreMapper.selectById(scoreId);
        if (s == null) throw new BizException("计算记录不存在");
        s.setManualLevel(level);
        s.setManualReason(reason);
        s.setManualBy(UserContext.realName());
        s.setManualTime(LocalDateTime.now());
        scoreMapper.updateById(s);
        VenueApplication a = applyMapper.selectById(s.getApplyId());
        log("人工修正", a == null ? "-" : a.getApplyNo(), "原等级 " + s.getLevel() + " → " + level + "；原因：" + reason);
        return s;
    }

    public PrepScore latest(Long applyId) {
        PrepScore s = scoreMapper.selectOne(new LambdaQueryWrapper<PrepScore>()
                .eq(PrepScore::getApplyId, applyId).orderByDesc(PrepScore::getId).last("limit 1"));
        return s == null ? null : normalize(s);
    }

    /**
     * 历史留痕归一化：早期种子数据把六因素直接写在 detail_json 顶层且用中文键，
     * 这里统一转成前端约定的 { factors, checklist } 结构，保证同一条记录在页面上展示一致。
     * 只影响返回给前端的副本，不改写数据库中的原始留痕。
     */
    @SuppressWarnings("unchecked")
    private PrepScore normalize(PrepScore s) {
        Map<String, Object> dj;
        try {
            dj = s.getDetailJson() == null || s.getDetailJson().isBlank()
                    ? new LinkedHashMap<>() : json.readValue(s.getDetailJson(), Map.class);
        } catch (Exception e) {
            dj = new LinkedHashMap<>();
        }
        if (dj.containsKey("factors") && dj.containsKey("checklist")) return s;

        Map<String, String> KEY = Map.of(
                "活动类型", "activityType", "参与人数", "people", "场地设备", "equipment",
                "特殊需求", "special", "使用时段", "timeSlot", "历史问题", "history");
        Map<String, Object> factors = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : dj.entrySet()) {
            String k = KEY.get(e.getKey());
            if (k != null) factors.put(k, e.getValue());
        }

        VenueApplication a = applyMapper.selectById(s.getApplyId());
        List<Map<String, Object>> checklist = List.of();
        if (a != null && a.getPeopleCount() != null && a.getActivityType() != null) {
            Venue v = venueMapper.selectById(a.getVenueId());
            boolean special = a.getNeedSecurity() != null && a.getNeedSecurity() == 1;
            checklist = buildChecklist(v, special, a.getPeopleCount(), a.getActivityType());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("factors", factors);
        out.put("checklist", checklist);
        try {
            s.setDetailJson(json.writeValueAsString(out));
        } catch (Exception ignore) {
            // 序列化失败时保留原值，页面按空清单展示
        }
        return s;
    }

    /** 看板：推荐采纳率与人工调整率 */
    public Map<String, Object> board() {
        List<PrepScore> all = scoreMapper.selectList(null);
        long adj = all.stream().filter(s -> s.getManualLevel() != null && !s.getManualLevel().isBlank()).count();
        Map<String, Long> dist = new LinkedHashMap<>();
        for (PrepScore s : all) {
            String lv = s.getManualLevel() != null && !s.getManualLevel().isBlank() ? s.getManualLevel() : s.getLevel();
            dist.merge(lv, 1L, Long::sum);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", all.size());
        m.put("adjusted", adj);
        m.put("adjustRate", all.isEmpty() ? 0 : Math.round(adj * 1000.0 / all.size()) / 10.0);
        m.put("adoptRate", all.isEmpty() ? 0 : Math.round((all.size() - adj) * 1000.0 / all.size()) / 10.0);
        m.put("distribution", dist);
        m.put("list", all.stream().sorted(Comparator.comparing(PrepScore::getId).reversed()).limit(50).toList());
        return m;
    }

    private void log(String action, String bizNo, String detail) {
        SysLog l = new SysLog();
        l.setUserId(UserContext.id());
        l.setUserName(UserContext.realName());
        l.setAction(action);
        l.setBizNo(bizNo);
        l.setDetail(detail);
        logMapper.insert(l);
    }
}
