package com.grad.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.venue.common.BizException;
import com.grad.venue.common.PageResult;
import com.grad.venue.common.UserContext;
import com.grad.venue.entity.*;
import com.grad.venue.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 借用申请核心服务。
 * 状态机：草稿 → 待院系审批 → 待场地审批 →（待安保审批）→ 待交接 → 已入场 → 活动中 → 待验收 → 已完成。
 * 所有流转都在后端校验，前端按钮只做提示。
 */
@Service
public class ApplyService {

    /** 会占用档期的状态，草稿/驳回/取消不占档期 */
    private static final List<String> OCCUPY = List.of("待院系审批", "待场地审批", "待安保审批", "待交接",
            "已入场", "活动中", "待验收", "已完成");

    private final VenueApplicationMapper applyMapper;
    private final VenueMapper venueMapper;
    private final ApplicationApprovalMapper approvalMapper;
    private final ItemReservationMapper reservationMapper;
    private final VenueItemMapper itemMapper;
    private final HandoverRecordMapper handoverMapper;
    private final PatrolRecordMapper patrolMapper;
    private final VenueAcceptanceMapper acceptanceMapper;
    private final FeeRecordMapper feeMapper;
    private final SysUserMapper userMapper;
    private final SysLogMapper logMapper;
    private final SysMessageMapper msgMapper;

    public ApplyService(VenueApplicationMapper applyMapper, VenueMapper venueMapper,
                        ApplicationApprovalMapper approvalMapper, ItemReservationMapper reservationMapper,
                        VenueItemMapper itemMapper, HandoverRecordMapper handoverMapper,
                        PatrolRecordMapper patrolMapper, VenueAcceptanceMapper acceptanceMapper,
                        FeeRecordMapper feeMapper, SysUserMapper userMapper, SysLogMapper logMapper,
                        SysMessageMapper msgMapper) {
        this.applyMapper = applyMapper;
        this.venueMapper = venueMapper;
        this.approvalMapper = approvalMapper;
        this.reservationMapper = reservationMapper;
        this.itemMapper = itemMapper;
        this.handoverMapper = handoverMapper;
        this.patrolMapper = patrolMapper;
        this.acceptanceMapper = acceptanceMapper;
        this.feeMapper = feeMapper;
        this.userMapper = userMapper;
        this.logMapper = logMapper;
        this.msgMapper = msgMapper;
    }

    /** 列表：默认按更新时间倒序，申请人只看本人，院系审批人看本院系 */
    public PageResult<VenueApplication> page(long page, long size, String keyword, String status, String group,
                                             String activityType, Long venueId) {
        LambdaQueryWrapper<VenueApplication> w = new LambdaQueryWrapper<>();
        String role = UserContext.role();
        if ("applicant".equals(role)) {
            w.eq(VenueApplication::getApplicantId, UserContext.id());
        } else if ("dept".equals(role)) {
            SysUser me = userMapper.selectById(UserContext.id());
            if (me != null && me.getDeptName() != null) w.eq(VenueApplication::getDeptName, me.getDeptName());
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(VenueApplication::getTitle, keyword).or().like(VenueApplication::getApplyNo, keyword));
        }
        if (status != null && !status.isBlank()) w.eq(VenueApplication::getStatus, status);
        if (activityType != null && !activityType.isBlank()) w.eq(VenueApplication::getActivityType, activityType);
        if (venueId != null) w.eq(VenueApplication::getVenueId, venueId);
        if (group != null && !group.isBlank()) {
            switch (group) {
                // 待办按角色所处审批节点收敛：院系/场地/安保只看到轮到自己处理的单子
                case "todo" -> {
                    String node = switch (role) {
                        case "dept" -> "待院系审批";
                        case "venue" -> "待场地审批";
                        case "security" -> "待安保审批";
                        case "finance" -> "待验收";
                        default -> null;
                    };
                    if (node != null) {
                        w.eq(VenueApplication::getStatus, node);
                    } else if ("applicant".equals(role)) {
                        w.in(VenueApplication::getStatus, List.of("草稿", "已驳回"));
                    } else {
                        w.in(VenueApplication::getStatus, List.of("待院系审批", "待场地审批", "待安保审批"));
                    }
                }
                case "doing" -> w.in(VenueApplication::getStatus, List.of("待交接", "已入场", "活动中", "待验收"));
                case "done" -> w.eq(VenueApplication::getStatus, "已完成");
                case "error" -> w.in(VenueApplication::getStatus, List.of("已驳回", "已取消"));
                default -> { }
            }
        }
        w.orderByDesc(VenueApplication::getUpdateTime).orderByDesc(VenueApplication::getId);
        PageResult<VenueApplication> pr = PageResult.of(applyMapper.selectPage(new Page<>(page, size), w));
        pr.getList().forEach(this::fillFlags);
        return pr;
    }

    /** 列表行附加标记：是否超期、是否可提交创新计算 */
    private void fillFlags(VenueApplication a) {
        a.setCancelFlag(a.getDeadline() != null && a.getDeadline().isBefore(LocalDateTime.now())
                && List.of("待院系审批", "待场地审批", "待安保审批").contains(a.getStatus()) ? 1 : 0);
    }

    /** 详情：基础信息 + 审批 + 预留 + 交接 + 巡场 + 验收 + 费用 + 时间轴 */
    public Map<String, Object> detail(Long id) {
        VenueApplication a = mustGet(id);
        Map<String, Object> m = new HashMap<>();
        m.put("apply", a);
        m.put("venue", venueMapper.selectById(a.getVenueId()));
        List<ApplicationApproval> approvals = approvalMapper.selectList(new LambdaQueryWrapper<ApplicationApproval>()
                .eq(ApplicationApproval::getApplyId, id).orderByAsc(ApplicationApproval::getCreateTime));
        m.put("approvals", approvals);
        m.put("reservations", reservationMapper.selectList(new LambdaQueryWrapper<ItemReservation>()
                .eq(ItemReservation::getApplyId, id)));
        m.put("handover", handoverMapper.selectOne(new LambdaQueryWrapper<HandoverRecord>()
                .eq(HandoverRecord::getApplyId, id).orderByDesc(HandoverRecord::getId).last("limit 1")));
        m.put("patrols", patrolMapper.selectList(new LambdaQueryWrapper<PatrolRecord>()
                .eq(PatrolRecord::getApplyId, id).orderByDesc(PatrolRecord::getCreateTime)));
        m.put("acceptance", acceptanceMapper.selectOne(new LambdaQueryWrapper<VenueAcceptance>()
                .eq(VenueAcceptance::getApplyId, id).orderByDesc(VenueAcceptance::getId).last("limit 1")));
        m.put("fees", feeMapper.selectList(new LambdaQueryWrapper<FeeRecord>()
                .eq(FeeRecord::getApplyId, id).orderByAsc(FeeRecord::getId)));
        // 时间轴：审批事件 + 关键节点，普通用户不可修改
        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(event("创建申请", a.getCreateTime(), a.getApplicantName(), "创建", "状态：草稿"));
        for (ApplicationApproval ap : approvals) {
            timeline.add(event(ap.getNode(), ap.getCreateTime(), ap.getApproverName(), ap.getResult(), ap.getOpinion()));
        }
        m.put("timeline", timeline);
        return m;
    }

    private Map<String, Object> event(String node, LocalDateTime t, String who, String result, String remark) {
        Map<String, Object> e = new HashMap<>();
        e.put("node", node);
        e.put("time", t);
        e.put("who", who);
        e.put("result", result);
        e.put("remark", remark);
        return e;
    }

    public VenueApplication mustGet(Long id) {
        VenueApplication a = applyMapper.selectById(id);
        if (a == null) throw new BizException("申请不存在");
        return a;
    }

    /** 新建草稿/保存：编号后端生成，前端只读 */
    @Transactional
    public VenueApplication save(VenueApplication a) {
        if (a.getVenueId() == null) throw new BizException("请选择场地");
        Venue v = venueMapper.selectById(a.getVenueId());
        if (v == null) throw new BizException("场地不存在");
        if (!"可用".equals(v.getStatus())) throw new BizException("该场地当前状态为「" + v.getStatus() + "」，不可借用");
        a.setVenueName(v.getName());
        if (a.getPeopleCount() != null && v.getCapacity() != null && a.getPeopleCount() > v.getCapacity() * 2) {
            throw new BizException("参与人数远超场地容量，请更换场地");
        }
        if (a.getId() == null) {
            a.setApplyNo(nextNo());
            a.setApplicantId(UserContext.id());
            a.setApplicantName(UserContext.realName());
            a.setStatus("草稿");
            a.setCurrentNode("申请提交");
            a.setHandlerName(UserContext.realName());
            a.setRiskLevel(riskOf(a));
            a.setNeedSecurity(needSecurity(a) ? 1 : 0);
            a.setVersion(0);
            applyMapper.insert(a);
        } else {
            VenueApplication old = applyMapper.selectById(a.getId());
            if (old == null) throw new BizException("申请不存在");
            if (!List.of("草稿", "待院系审批").contains(old.getStatus())) {
                throw new BizException("当前状态不允许编辑，请先退回");
            }
            if (a.getVersion() != null && old.getVersion() != null && !a.getVersion().equals(old.getVersion())) {
                throw new BizException(409, "数据已被他人更新，请刷新后重试");
            }
            // 方案变更：人数、场地、时间变化时重新评估审批范围
            a.setRiskLevel(riskOf(a));
            a.setNeedSecurity(needSecurity(a) ? 1 : 0);
            applyMapper.updateById(a);
        }
        return a;
    }

    private String nextNo() {
        Long max = applyMapper.selectCount(null);
        return String.format("VA-2026-%04d", (max == null ? 0 : max) + 1);
    }

    /** 风险等级：人数、户外、夜间、用电搭建取最高 */
    private String riskOf(VenueApplication a) {
        if (a.getPeopleCount() != null && a.getPeopleCount() > 500) return "大型";
        Venue v = venueMapper.selectById(a.getVenueId());
        if (v != null && (v.getAllowActivity() != null && v.getName().contains("户外"))) return "户外";
        if (a.getStartTime() != null && a.getStartTime().getHour() >= 18) return "夜间";
        if (a.getEquipNeed() != null && a.getEquipNeed().contains("用电")) return "用电搭建";
        return "普通";
    }

    /** 动态审批判定：超 500 人、户外、夜间（18:00 后）、用电搭建需追加安保 */
    private boolean needSecurity(VenueApplication a) {
        if (a.getPeopleCount() != null && a.getPeopleCount() > 500) return true;
        if (a.getStartTime() != null && a.getStartTime().getHour() >= 18) return true;
        Venue v = venueMapper.selectById(a.getVenueId());
        if (v != null) {
            String n = v.getName() + v.getAllowActivity();
            if (n.contains("户外") || n.contains("场") && n.contains("外")) return true;
        }
        String eq = a.getEquipNeed() == null ? "" : a.getEquipNeed();
        return eq.contains("用电") || eq.contains("搭建");
    }

    /** 时段冲突：范围取布场开始 → 撤场结束，与同场地其他占用单比较 */
    public List<VenueApplication> conflicts(VenueApplication a) {
        LocalDateTime s = a.getSetupStart() != null ? a.getSetupStart() : a.getStartTime();
        LocalDateTime e = a.getTeardownEnd() != null ? a.getTeardownEnd() : a.getEndTime();
        if (s == null || e == null) return List.of();
        LambdaQueryWrapper<VenueApplication> w = new LambdaQueryWrapper<VenueApplication>()
                .eq(VenueApplication::getVenueId, a.getVenueId())
                .in(VenueApplication::getStatus, OCCUPY)
                .isNotNull(VenueApplication::getStartTime);
        if (a.getId() != null) w.ne(VenueApplication::getId, a.getId());
        List<VenueApplication> list = applyMapper.selectList(w);
        List<VenueApplication> hit = new ArrayList<>();
        for (VenueApplication o : list) {
            LocalDateTime os = o.getSetupStart() != null ? o.getSetupStart() : o.getStartTime();
            LocalDateTime oe = o.getTeardownEnd() != null ? o.getTeardownEnd() : o.getEndTime();
            if (os == null || oe == null) continue;
            if (s.isBefore(oe) && os.isBefore(e)) hit.add(o);
        }
        return hit;
    }

    /** 未结损坏赔偿锁：有未收赔偿的申请人不可提交新申请 */
    public List<FeeRecord> unpaidDamage(Long applicantId) {
        List<VenueApplication> mine = applyMapper.selectList(new LambdaQueryWrapper<VenueApplication>()
                .eq(VenueApplication::getApplicantId, applicantId).select(VenueApplication::getId));
        if (mine.isEmpty()) return List.of();
        List<Long> ids = mine.stream().map(VenueApplication::getId).toList();
        return feeMapper.selectList(new LambdaQueryWrapper<FeeRecord>()
                .in(FeeRecord::getApplyId, ids)
                .eq(FeeRecord::getFeeType, "损坏赔偿")
                .eq(FeeRecord::getPayStatus, "未收"));
    }

    /** 提交审批 */
    @Transactional
    public VenueApplication submit(Long id) {
        VenueApplication a = mustGet(id);
        if (!"草稿".equals(a.getStatus())) throw new BizException("只有草稿可以提交");
        if (a.getStartTime() == null || a.getEndTime() == null) throw new BizException("请填写活动时间");
        if (a.getStartTime().isAfter(a.getEndTime())) throw new BizException("活动开始时间不能晚于结束时间");
        List<FeeRecord> unpaid = unpaidDamage(a.getApplicantId());
        if (!unpaid.isEmpty()) {
            BigDecimal sum = unpaid.stream().map(FeeRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            throw new BizException("存在未结损坏赔偿 ¥" + sum + "，完成赔偿前不可提交新申请");
        }
        List<VenueApplication> hit = conflicts(a);
        if (!hit.isEmpty()) {
            throw new BizException("时段冲突：与 " + hit.get(0).getApplyNo() + " 重叠（含布场/撤场）");
        }
        a.setRiskLevel(riskOf(a));
        a.setNeedSecurity(needSecurity(a) ? 1 : 0);
        move(a, "待院系审批", "院系审批", findUserName("dept", a.getDeptName()));
        a.setDeadline(LocalDateTime.now().plusDays(2));
        applyMapper.updateById(a);
        log("提交申请", a.getApplyNo(), "草稿 → 待院系审批");
        push(a, "待审批提醒", "申请 " + a.getApplyNo() + "「" + a.getTitle() + "」待院系审批", "/approval?id=" + a.getId());
        return a;
    }

    private String findUserName(String role, String dept) {
        List<SysUser> us = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, role));
        for (SysUser u : us) if (dept != null && dept.equals(u.getDeptName())) return u.getRealName();
        return us.isEmpty() ? "待分配" : us.get(0).getRealName();
    }

    /**
     * 审批：节点由当前状态推出，不允许越级。
     * 院系通过 → 待场地审批；场地通过 → 需安保则待安保审批，否则待交接；安保通过 → 待交接。
     */
    @Transactional
    public VenueApplication approve(Long id, String result, String opinion) {
        VenueApplication a = mustGet(id);
        if (opinion != null && ("退回".equals(result) || "驳回".equals(result)) && opinion.trim().length() < 5) {
            throw new BizException("退回/驳回必须填写不少于 5 个字的原因");
        }
        String node = switch (a.getStatus()) {
            case "待院系审批" -> "院系审批";
            case "待场地审批" -> "场地审核";
            case "待安保审批" -> "安保审批";
            default -> throw new BizException("当前状态「" + a.getStatus() + "」不允许审批");
        };
        // 角色与节点匹配，防止越级调用
        String role = UserContext.role();
        boolean ok = switch (node) {
            case "院系审批" -> "dept".equals(role) || "admin".equals(role);
            case "场地审核" -> "venue".equals(role) || "admin".equals(role);
            default -> "security".equals(role) || "admin".equals(role);
        };
        if (!ok) throw new BizException(403, "无权处理「" + node + "」节点");
        Long dup = approvalMapper.selectCount(new LambdaQueryWrapper<ApplicationApproval>()
                .eq(ApplicationApproval::getApplyId, id).eq(ApplicationApproval::getNode, node)
                .eq(ApplicationApproval::getResult, "通过"));
        if (dup != null && dup > 0) throw new BizException("该节点已审批，请勿重复操作");

        ApplicationApproval ap = new ApplicationApproval();
        ap.setApplyId(id);
        ap.setNode(node);
        ap.setApproverId(UserContext.id());
        ap.setApproverName(UserContext.realName());
        ap.setResult(result);
        ap.setOpinion(opinion == null ? "同意" : opinion);
        long hours = a.getCreateTime() == null ? 1 : Math.max(1, Duration.between(a.getCreateTime(), LocalDateTime.now()).toHours());
        ap.setCostHours(BigDecimal.valueOf(hours));
        approvalMapper.insert(ap);

        if ("退回".equals(result)) {
            move(a, "草稿", "申请提交", a.getApplicantName());
            a.setRejectReason(opinion);
            a.setDeadline(null);
            push(a, "已退回", "申请 " + a.getApplyNo() + " 被" + node + "退回：" + opinion, "/apply?id=" + id);
        } else if ("驳回".equals(result)) {
            move(a, "已驳回", "归档", node);
            a.setRejectReason(opinion);
            a.setDeadline(null);
            push(a, "已驳回", "申请 " + a.getApplyNo() + " 被驳回：" + opinion, "/apply?id=" + id);
        } else {
            switch (node) {
                case "院系审批" -> {
                    move(a, "待场地审批", "场地审核", findUserName("venue", null));
                    a.setDeadline(LocalDateTime.now().plusDays(2));
                }
                case "场地审核" -> {
                    if (a.getNeedSecurity() != null && a.getNeedSecurity() == 1) {
                        move(a, "待安保审批", "安保审批", findUserName("security", null));
                        a.setDeadline(LocalDateTime.now().plusDays(1));
                    } else {
                        move(a, "待交接", "准备执行", findUserName("venue", null));
                        a.setDeadline(a.getSetupStart());
                    }
                }
                default -> {
                    move(a, "待交接", "准备执行", findUserName("venue", null));
                    a.setDeadline(a.getSetupStart());
                }
            }
            push(a, "审批通过", "申请 " + a.getApplyNo() + " 已通过" + node, "/apply?id=" + id);
        }
        applyMapper.updateById(a);
        log(node + result, a.getApplyNo(), "状态 → " + a.getStatus() + "；意见：" + ap.getOpinion());
        return a;
    }

    /** 通用流转：入场、开始、取消等，统一做前置状态校验 */
    @Transactional
    public VenueApplication transition(Long id, String target, String reason) {
        VenueApplication a = mustGet(id);
        String cur = a.getStatus();
        switch (target) {
            case "活动开始" -> {
                if (!"已入场".equals(cur)) throw new BizException("只有已入场可开始活动");
                move(a, "活动中", "使用确认", UserContext.realName());
            }
            case "提交验收" -> {
                if (!List.of("活动中", "已入场").contains(cur)) throw new BizException("当前状态不可提交验收");
                move(a, "待验收", "使用确认", findUserName("venue", null));
            }
            case "取消" -> {
                if (List.of("已完成", "已取消").contains(cur)) throw new BizException("当前状态不可取消");
                if (reason == null || reason.trim().length() < 5) throw new BizException("取消必须填写不少于 5 个字的原因");
                move(a, "已取消", "归档", a.getApplicantName());
                a.setRejectReason(reason);
            }
            default -> throw new BizException("不支持的目标状态：" + target);
        }
        applyMapper.updateById(a);
        log("状态流转", a.getApplyNo(), cur + " → " + a.getStatus() + "；原因：" + (reason == null ? "无" : reason));
        return a;
    }

    /** 物资预留：同场地同时段累计不得超过库存 */
    @Transactional
    public ItemReservation reserve(Long applyId, Long itemId, Integer qty) {
        VenueApplication a = mustGet(applyId);
        VenueItem it = itemMapper.selectById(itemId);
        if (it == null) throw new BizException("物资不存在");
        if (qty == null || qty <= 0) throw new BizException("预留数量必须大于 0");
        int used = 0;
        List<VenueApplication> same = applyMapper.selectList(new LambdaQueryWrapper<VenueApplication>()
                .eq(VenueApplication::getVenueId, a.getVenueId())
                .in(VenueApplication::getStatus, List.of("待交接", "已入场", "活动中", "待验收")));
        for (VenueApplication o : same) {
            LocalDateTime os = o.getSetupStart() != null ? o.getSetupStart() : o.getStartTime();
            LocalDateTime oe = o.getTeardownEnd() != null ? o.getTeardownEnd() : o.getEndTime();
            LocalDateTime s = a.getSetupStart() != null ? a.getSetupStart() : a.getStartTime();
            LocalDateTime e = a.getTeardownEnd() != null ? a.getTeardownEnd() : a.getEndTime();
            if (os == null || oe == null || s == null || e == null) continue;
            if (!(s.isBefore(oe) && os.isBefore(e))) continue;
            List<ItemReservation> rs = reservationMapper.selectList(new LambdaQueryWrapper<ItemReservation>()
                    .eq(ItemReservation::getApplyId, o.getId()).eq(ItemReservation::getItemId, itemId)
                    .in(ItemReservation::getStatus, List.of("预留", "已交付")));
            used += rs.stream().mapToInt(ItemReservation::getQty).sum();
        }
        if (used + qty > it.getTotalQty()) {
            throw new BizException("物资「" + it.getItemName() + "」同时段仅剩 " + (it.getTotalQty() - used) + " " + it.getUnit());
        }
        ItemReservation r = new ItemReservation();
        r.setApplyId(applyId);
        r.setItemId(itemId);
        r.setItemName(it.getItemName());
        r.setQty(qty);
        r.setStatus("预留");
        reservationMapper.insert(r);
        log("物资预留", a.getApplyNo(), "预留 " + it.getItemName() + " " + qty + " " + it.getUnit());
        return r;
    }

    /** 日历：按场地与月份返回占用时段，含布场与撤场 */
    public List<Map<String, Object>> calendar(Long venueId, String month) {
        LambdaQueryWrapper<VenueApplication> w = new LambdaQueryWrapper<VenueApplication>()
                .in(VenueApplication::getStatus, OCCUPY).isNotNull(VenueApplication::getStartTime);
        if (venueId != null) w.eq(VenueApplication::getVenueId, venueId);
        if (month != null && !month.isBlank()) {
            LocalDateTime s = LocalDateTime.parse(month + "-01T00:00:00");
            w.ge(VenueApplication::getSetupStart, s.withDayOfMonth(1)).lt(VenueApplication::getSetupStart, s.plusMonths(1));
        }
        List<VenueApplication> list = applyMapper.selectList(w.orderByAsc(VenueApplication::getSetupStart));
        List<Map<String, Object>> res = new ArrayList<>();
        for (VenueApplication a : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("no", a.getApplyNo());
            m.put("title", a.getTitle());
            m.put("venue", a.getVenueName());
            m.put("status", a.getStatus());
            m.put("setup", a.getSetupStart());
            m.put("start", a.getStartTime());
            m.put("end", a.getEndTime());
            m.put("teardown", a.getTeardownEnd());
            res.add(m);
        }
        return res;
    }

    private void move(VenueApplication a, String status, String node, String handler) {
        a.setStatus(status);
        a.setCurrentNode(node);
        a.setHandlerName(handler);
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

    private void push(VenueApplication a, String title, String content, String url) {
        SysMessage m = new SysMessage();
        m.setTitle(title);
        m.setContent(content);
        m.setBizNo(a.getApplyNo());
        m.setBizType("审批");
        m.setJumpUrl(url);
        m.setReceiverId(a.getApplicantId());
        m.setReadFlag(0);
        msgMapper.insert(m);
    }
}
