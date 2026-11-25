package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息：包含业务编号与详情跳转地址 */
@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String bizNo;
    private String bizType;
    private String jumpUrl;
    private Long receiverId;
    private Integer readFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
