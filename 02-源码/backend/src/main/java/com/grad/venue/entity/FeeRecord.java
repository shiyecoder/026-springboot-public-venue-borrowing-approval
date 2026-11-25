package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 线下费用：场地费、押金、物资费、损坏赔偿，不做线上支付 */
@Data
@TableName("fee_record")
public class FeeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    /** 关联申请编号（列表展示用，非表字段） */
    @TableField(exist = false)
    private String applyNo;
    private String feeType;
    private BigDecimal amount;
    private String payStatus;
    private String handler;
    private LocalDateTime payTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
