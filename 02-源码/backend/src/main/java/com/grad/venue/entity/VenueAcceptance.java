package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 撤场验收：清洁、设施、物资、钥匙、损坏与赔偿，损坏需管理员复核 */
@Data
@TableName("venue_acceptance")
public class VenueAcceptance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    /** 关联申请编号（列表展示用，非表字段） */
    @TableField(exist = false)
    private String applyNo;
    private Integer cleanerOk;
    private Integer facilityOk;
    private Integer itemOk;
    private Integer keyReturned;
    private Integer damageFlag;
    private String damageDesc;
    private BigDecimal compensateAmount;
    private String reviewStatus;
    private String result;
    private String photo;
    private String acceptor;
    private LocalDateTime acceptTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
