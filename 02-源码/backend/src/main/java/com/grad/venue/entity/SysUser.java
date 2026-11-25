package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户：BCrypt 保存密码，接口与日志均不返回密码字段 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String realName;
    private String role;
    private String roleName;
    private String phone;
    private String avatar;
    private String deptName;
    private Integer status;
    private LocalDateTime lastLogin;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
