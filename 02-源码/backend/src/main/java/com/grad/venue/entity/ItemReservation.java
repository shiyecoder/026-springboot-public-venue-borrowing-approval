package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 物资预留明细：数量与同场地同时段其他申请合并校验 */
@Data
@TableName("item_reservation")
public class ItemReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private Long itemId;
    private String itemName;
    private Integer qty;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
