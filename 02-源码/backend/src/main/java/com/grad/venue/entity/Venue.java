package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 校内公共场地主档：容量、设备、费用标准与现场图片 */
@Data
@TableName("venue")
public class Venue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String campus;
    private String building;
    private Integer capacity;
    private BigDecimal area;
    private String allowActivity;
    private String equipment;
    private String openTime;
    private String deptName;
    private BigDecimal feeStandard;
    private String status;
    private String image;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
