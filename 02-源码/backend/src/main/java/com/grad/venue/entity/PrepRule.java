package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 准备清单权重规则：六项因素合计必须 100%，修改记版本 */
@Data
@TableName("prep_rule")
public class PrepRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleKey;
    private String ruleName;
    private BigDecimal weight;
    private String version;
    private Integer enabled;
    private LocalDateTime effectiveTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
