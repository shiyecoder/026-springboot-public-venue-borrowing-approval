package com.grad.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.venue.entity.*;
import com.grad.venue.mapper.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** 场地利用看板：6 个核心指标 + 6 组图表数据，全部后端聚合，前端不再二次统计 */
@Service
public class DashboardService {

    private final VenueMapper venueMapper;
    private final VenueApplicationMapper applyMapper;
    private final VenueAcceptanceMapper acceptanceMapper;
    private final PrepScoreMapper scoreMapper;
    private final ApplicationApprovalMapper approvalMapper;

    public DashboardService(VenueMapper venueMapper, VenueApplicationMapper applyMapper,
                            VenueAcceptanceMapper acceptanceMapper, PrepScoreMapper scoreMapper,
                            ApplicationApprovalMapper approvalMapper) {
        this.venueMapper = venueMapper;
        this.applyMapper = applyMapper;
        this.acceptanceMapper = acceptanceMapper;
        this.scoreMapper = scoreMapper;
        this.approvalMapper = approvalMapper;
    }

    public Map<String, Object> index(String range) {
        List<VenueApplication> all = applyMapper.selectList(null);
        long total = all.size();
        long finished = all.stream().filter(a -> "已完成".equals(a.getStatus())).count();
        long cancelled = all.stream().filter(a -> "已取消".equals(a.getStatus())).count();
        long rejected = all.stream().filter(a -> "已驳回".equals(a.getStatus())).count();
        long venues = venueMapper.selectCount(null);

        Map<String, Object> m = new LinkedHashMap<>();
        // 核心指标
        m.put("venueCount", venues);
        m.put("applyCount", total);
        m.put("utilization", venues == 0 ? 0 : Math.round(finished * 1000.0 / (venues * 3)) / 10.0);
        m.put("cancelRate", total == 0 ? 0 : Math.round(cancelled * 1000.0 / total) / 10.0);
        m.put("conflictCount", rejected);
        m.put("prepDoneRate", total == 0 ? 0 : Math.round(finished * 1000.0 / Math.max(1, finished + rejected)) / 10.0);

        // 图表 1：近 6 个月申请趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        String[] months = {"2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10"};
        for (String mo : months) {
            long apply = all.stream().filter(a -> a.getCreateTime() != null && a.getCreateTime().toString().startsWith(mo)).count();
            long done = all.stream().filter(a -> "已完成".equals(a.getStatus()) && a.getCreateTime() != null
                    && a.getCreateTime().toString().startsWith(mo)).count();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("month", mo);
            r.put("apply", apply);
            r.put("done", done);
            trend.add(r);
        }
        m.put("trend", trend);

        // 图表 2：状态分布
        Map<String, Long> status = new LinkedHashMap<>();
        for (VenueApplication a : all) status.merge(a.getStatus(), 1L, Long::sum);
        m.put("statusDist", status.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 图表 3：活动类型分布
        Map<String, Long> types = new LinkedHashMap<>();
        for (VenueApplication a : all) types.merge(a.getActivityType(), 1L, Long::sum);
        m.put("typeDist", types.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 图表 4：院系使用量 Top5
        Map<String, Long> depts = new LinkedHashMap<>();
        for (VenueApplication a : all) depts.merge(a.getDeptName(), 1L, Long::sum);
        List<Map<String, Object>> deptTop = depts.entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(5)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue())).toList();
        m.put("deptTop", deptTop);

        // 图表 5：审批时效（按节点平均耗时）
        List<ApplicationApproval> apprs = approvalMapper.selectList(null);
        Map<String, List<BigDecimal>> cost = new LinkedHashMap<>();
        for (ApplicationApproval ap : apprs) {
            if (ap.getCostHours() != null) cost.computeIfAbsent(ap.getNode(), k -> new ArrayList<>()).add(ap.getCostHours());
        }
        List<Map<String, Object>> efficiency = new ArrayList<>();
        cost.forEach((k, v) -> {
            double avg = v.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("node", k);
            r.put("hours", Math.round(avg * 10) / 10.0);
            efficiency.add(r);
        });
        m.put("efficiency", efficiency);

        // 图表 6：创新结果分布 + 采纳率
        List<PrepScore> scores = scoreMapper.selectList(null);
        Map<String, Long> lv = new LinkedHashMap<>();
        for (PrepScore s : scores) {
            String k = s.getManualLevel() != null && !s.getManualLevel().isBlank() ? s.getManualLevel() : s.getLevel();
            lv.merge(k, 1L, Long::sum);
        }
        long adj = scores.stream().filter(s -> s.getManualLevel() != null).count();
        m.put("prepDist", lv.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());
        m.put("adoptRate", scores.isEmpty() ? 0 : Math.round((scores.size() - adj) * 1000.0 / scores.size()) / 10.0);

        // 热门场地 Top5
        Map<String, Long> hot = new LinkedHashMap<>();
        for (VenueApplication a : all) hot.merge(a.getVenueName(), 1L, Long::sum);
        m.put("hotVenue", hot.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(5)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue())).toList());

        // 损坏赔偿
        List<VenueAcceptance> accs = acceptanceMapper.selectList(null);
        BigDecimal damage = accs.stream().filter(x -> x.getDamageFlag() != null && x.getDamageFlag() == 1)
                .map(VenueAcceptance::getCompensateAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("damageTotal", damage);
        m.put("updateTime", LocalDateTime.now());
        return m;
    }
}
