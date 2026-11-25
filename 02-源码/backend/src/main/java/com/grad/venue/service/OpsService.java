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
import java.time.LocalDateTime;
import java.util.List;

/** 现场作业：入场交接、活动巡场、撤场验收、线下费用 */
@Service
public class OpsService {

    private final HandoverRecordMapper handoverMapper;
    private final PatrolRecordMapper patrolMapper;
    private final VenueAcceptanceMapper acceptanceMapper;
    private final FeeRecordMapper feeMapper;
    private final VenueApplicationMapper applyMapper;
    private final ItemReservationMapper reservationMapper;
    private final SysLogMapper logMapper;

    public OpsService(HandoverRecordMapper handoverMapper, PatrolRecordMapper patrolMapper,
                      VenueAcceptanceMapper acceptanceMapper, FeeRecordMapper feeMapper,
                      VenueApplicationMapper applyMapper, ItemReservationMapper reservationMapper,
                      SysLogMapper logMapper) {
        this.handoverMapper = handoverMapper;
        this.patrolMapper = patrolMapper;
        this.acceptanceMapper = acceptanceMapper;
        this.feeMapper = feeMapper;
        this.applyMapper = applyMapper;
        this.reservationMapper = reservationMapper;
        this.logMapper = logMapper;
    }

    private VenueApplication mustGet(Long id) {
        VenueApplication a = applyMapper.selectById(id);
        if (a == null) throw new BizException("申请不存在");
        return a;
    }

    /** 入场交接：仅待交接可办理，双方确认后状态置为已入场 */
    @Transactional
    public HandoverRecord handover(Long applyId, HandoverRecord data) {
        VenueApplication a = mustGet(applyId);
        if (!"待交接".equals(a.getStatus())) throw new BizException("当前状态「" + a.getStatus() + "」不可办理交接");
        if (data.getApplicantConfirm() == null || data.getManagerConfirm() == null) {
            throw new BizException("需申请人与场地管理员双方确认");
        }
        data.setApplyId(applyId);
        if (data.getHandoverTime() == null) data.setHandoverTime(LocalDateTime.now());
        handoverMapper.insert(data);
        // 预留物资随交接转为已交付
        List<ItemReservation> rs = reservationMapper.selectList(new LambdaQueryWrapper<ItemReservation>()
                .eq(ItemReservation::getApplyId, applyId).eq(ItemReservation::getStatus, "预留"));
        for (ItemReservation r : rs) { r.setStatus("已交付"); reservationMapper.updateById(r); }
        a.setStatus("已入场");
        a.setCurrentNode("准备执行");
        applyMapper.updateById(a);
        log("入场交接", a.getApplyNo(), "电表示数 " + data.getElectricReading() + "，钥匙：" + data.getKeyInfo());
        return data;
    }

    /** 巡场：已入场或活动中可录，异常类型需写明处理结果 */
    @Transactional
    public PatrolRecord patrol(Long applyId, PatrolRecord data) {
        VenueApplication a = mustGet(applyId);
        if (!List.of("已入场", "活动中").contains(a.getStatus())) throw new BizException("仅活动进行中可记录巡场");
        data.setApplyId(applyId);
        if (data.getPatrolTime() == null) data.setPatrolTime(LocalDateTime.now());
        if (data.getPatrolUser() == null) data.setPatrolUser(UserContext.realName());
        patrolMapper.insert(data);
        if (!"正常".equals(data.getIssueType()) && (data.getHandleResult() == null || data.getHandleResult().isBlank())) {
            throw new BizException("异常巡场必须填写处理结果");
        }
        log("活动巡场", a.getApplyNo(), "问题类型：" + data.getIssueType() + "；" + data.getDescription());
        return data;
    }

    /** 撤场验收：通过则完成，不通过回到待验收；有损坏自动生成赔偿单并锁定押金 */
    @Transactional
    public VenueAcceptance accept(Long applyId, VenueAcceptance data) {
        VenueApplication a = mustGet(applyId);
        if (!List.of("活动中", "已入场", "待验收").contains(a.getStatus())) {
            throw new BizException("当前状态「" + a.getStatus() + "」不可验收");
        }
        data.setApplyId(applyId);
        data.setAcceptor(UserContext.realName());
        if (data.getAcceptTime() == null) data.setAcceptTime(LocalDateTime.now());
        boolean damaged = data.getDamageFlag() != null && data.getDamageFlag() == 1;
        if (damaged) {
            if (data.getDamageDesc() == null || data.getDamageDesc().trim().length() < 5) {
                throw new BizException("损坏说明不少于 5 个字");
            }
            if (data.getCompensateAmount() == null || data.getCompensateAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("请填写赔偿金额");
            }
            data.setReviewStatus("待复核");
            data.setResult("驳回");
        } else {
            data.setCompensateAmount(BigDecimal.ZERO);
            data.setReviewStatus("已确认");
            data.setResult("通过");
        }
        acceptanceMapper.insert(data);
        if (damaged) {
            a.setStatus("待验收");
            FeeRecord f = new FeeRecord();
            f.setApplyId(applyId);
            f.setFeeType("损坏赔偿");
            f.setAmount(data.getCompensateAmount());
            f.setPayStatus("未收");
            f.setHandler(UserContext.realName());
            f.setRemark(data.getDamageDesc());
            feeMapper.insert(f);
        } else {
            a.setStatus("已完成");
            a.setCurrentNode("归档");
        }
        applyMapper.updateById(a);
        log("撤场验收" + data.getResult(), a.getApplyNo(),
                damaged ? "损坏：" + data.getDamageDesc() + "，赔偿 " + data.getCompensateAmount() : "清洁设施物资钥匙均合格");
        return data;
    }

    /** 赔偿金额复核：确认后可释放押金 */
    @Transactional
    public void reviewAcceptance(Long id, BigDecimal amount, String remark) {
        VenueAcceptance acc = acceptanceMapper.selectById(id);
        if (acc == null) throw new BizException("验收记录不存在");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("复核金额必须大于 0");
        acc.setCompensateAmount(amount);
        acc.setReviewStatus("已确认");
        acc.setResult("通过");
        acc.setRemark(remark);
        acceptanceMapper.updateById(acc);
        // 同步赔偿单金额
        List<FeeRecord> fs = feeMapper.selectList(new LambdaQueryWrapper<FeeRecord>()
                .eq(FeeRecord::getApplyId, acc.getApplyId()).eq(FeeRecord::getFeeType, "损坏赔偿"));
        for (FeeRecord f : fs) {
            f.setAmount(amount);
            f.setPayStatus("未收");
            feeMapper.updateById(f);
        }
        VenueApplication a = mustGet(acc.getApplyId());
        a.setStatus("已完成");
        a.setCurrentNode("归档");
        applyMapper.updateById(a);
        log("赔偿复核", a.getApplyNo(), "复核金额 " + amount + "，验收通过");
    }

    public PageResult<VenueAcceptance> acceptPage(long page, long size) {
        PageResult<VenueAcceptance> pr = PageResult.of(acceptanceMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<VenueAcceptance>().orderByDesc(VenueAcceptance::getId)));
        fillApplyNo(pr.getList());
        return pr;
    }

    public PageResult<HandoverRecord> handoverPage(long page, long size) {
        PageResult<HandoverRecord> pr = PageResult.of(handoverMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<HandoverRecord>().orderByDesc(HandoverRecord::getId)));
        fillApplyNo(pr.getList());
        return pr;
    }

    public PageResult<PatrolRecord> patrolPage(long page, long size) {
        PageResult<PatrolRecord> pr = PageResult.of(patrolMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PatrolRecord>().orderByDesc(PatrolRecord::getId)));
        fillApplyNo(pr.getList());
        return pr;
    }

    public PageResult<FeeRecord> feePage(long page, long size, String feeType, String payStatus) {
        LambdaQueryWrapper<FeeRecord> w = new LambdaQueryWrapper<>();
        if (feeType != null && !feeType.isBlank()) w.eq(FeeRecord::getFeeType, feeType);
        if (payStatus != null && !payStatus.isBlank()) w.eq(FeeRecord::getPayStatus, payStatus);
        w.orderByDesc(FeeRecord::getId);
        PageResult<FeeRecord> pr = PageResult.of(feeMapper.selectPage(new Page<>(page, size), w));
        fillApplyNo(pr.getList());
        return pr;
    }

    /** 费用登记：仅线下记账，不触发线上支付 */
    @Transactional
    public FeeRecord saveFee(FeeRecord f) {
        if (f.getApplyId() == null) throw new BizException("请选择关联申请");
        if (f.getAmount() == null || f.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new BizException("金额必须大于 0");
        if (f.getHandler() == null) f.setHandler(UserContext.realName());
        if ("已收".equals(f.getPayStatus()) || "已退".equals(f.getPayStatus())) {
            if (f.getPayTime() == null) f.setPayTime(LocalDateTime.now());
        }
        if (f.getId() == null) feeMapper.insert(f); else feeMapper.updateById(f);
        VenueApplication a = mustGet(f.getApplyId());
        log("费用登记", a.getApplyNo(), f.getFeeType() + " " + f.getAmount() + "，" + f.getPayStatus());
        return f;
    }

    /** 列表补申请编号，避免前端只显示数据库主键 */
    private void fillApplyNo(List<?> list) {
        java.util.Map<Long, String> m = new java.util.HashMap<>();
        for (Object o : list) {
            Long id = applyIdOf(o);
            if (id != null) m.put(id, null);
        }
        if (m.isEmpty()) return;
        for (VenueApplication a : applyMapper.selectBatchIds(m.keySet())) m.put(a.getId(), a.getApplyNo());
        for (Object o : list) setApplyNo(o, m.get(applyIdOf(o)));
    }

    private Long applyIdOf(Object o) {
        if (o instanceof VenueAcceptance x) return x.getApplyId();
        if (o instanceof HandoverRecord x) return x.getApplyId();
        if (o instanceof PatrolRecord x) return x.getApplyId();
        if (o instanceof FeeRecord x) return x.getApplyId();
        return null;
    }

    private void setApplyNo(Object o, String no) {
        if (o instanceof VenueAcceptance x) x.setApplyNo(no);
        else if (o instanceof HandoverRecord x) x.setApplyNo(no);
        else if (o instanceof PatrolRecord x) x.setApplyNo(no);
        else if (o instanceof FeeRecord x) x.setApplyNo(no);
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
