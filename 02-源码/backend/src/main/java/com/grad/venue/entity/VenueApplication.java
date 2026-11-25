package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 场地借用申请：核心过程单据，状态由后端状态机驱动 */
@Data
@TableName("venue_application")
public class VenueApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private String title;
    private String activityType;
    private Long venueId;
    private String venueName;
    private Long applicantId;
    private String applicantName;
    private String deptName;
    private Integer peopleCount;
    private LocalDateTime setupStart;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime teardownEnd;
    private String processDesc;
    private String equipNeed;
    private String outsiderInfo;
    private String attachFile;
    private String riskLevel;
    private Integer needSecurity;
    private String status;
    private String currentNode;
    private String handlerName;
    private LocalDateTime deadline;
    private String rejectReason;
    private Integer cancelFlag;
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
