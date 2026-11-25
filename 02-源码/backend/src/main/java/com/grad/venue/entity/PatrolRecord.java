package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 活动巡场：超员、违规用电、噪音、通道堵塞 */
@Data
@TableName("patrol_record")
public class PatrolRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    /** 关联申请编号（列表展示用，非表字段） */
    @TableField(exist = false)
    private String applyNo;
    private LocalDateTime patrolTime;
    private String patrolUser;
    private String issueType;
    private Integer overCapacity;
    private Integer illegalPower;
    private Integer noiseLevel;
    private Integer aisleBlocked;
    private String description;
    private String photo;
    private String handleResult;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
