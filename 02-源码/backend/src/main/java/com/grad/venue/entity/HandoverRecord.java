package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 入场交接：场地现状、交付物资、电表示数、钥匙与双方确认 */
@Data
@TableName("handover_record")
public class HandoverRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    /** 关联申请编号（列表展示用，非表字段） */
    @TableField(exist = false)
    private String applyNo;
    private String venueState;
    private String itemsDelivered;
    private BigDecimal electricReading;
    private String keyInfo;
    private String photo;
    private String applicantConfirm;
    private String managerConfirm;
    private LocalDateTime handoverTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
