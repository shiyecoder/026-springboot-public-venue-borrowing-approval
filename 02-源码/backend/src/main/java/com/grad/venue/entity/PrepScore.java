package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 准备清单计算留痕：输入快照、明细分、等级、解释与人工修正 */
@Data
@TableName("prep_score")
public class PrepScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private String ruleVersion;
    private String inputSnapshot;
    private String detailJson;
    private BigDecimal totalScore;
    private String level;
    private String unmet;
    private String advice;
    private String manualLevel;
    private String manualReason;
    private String manualBy;
    private LocalDateTime manualTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
