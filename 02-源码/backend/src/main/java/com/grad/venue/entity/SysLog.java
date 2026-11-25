package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志：状态流转前后值、原因留痕，普通用户不可修改 */
@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userName;
    private String action;
    private String bizNo;
    private String detail;
    private String ip;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
