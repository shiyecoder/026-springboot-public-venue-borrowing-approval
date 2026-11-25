package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 审批留痕：节点、审批人、结果、意见与耗时 */
@Data
@TableName("application_approval")
public class ApplicationApproval {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private String node;
    private Long approverId;
    private String approverName;
    private String result;
    private String opinion;
    private BigDecimal costHours;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
