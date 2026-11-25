package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 场地物资库：桌椅、展板、音响等可预留物资 */
@Data
@TableName("venue_item")
public class VenueItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long venueId;
    private String itemName;
    private String itemType;
    private Integer totalQty;
    private String unit;
    private String status;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
