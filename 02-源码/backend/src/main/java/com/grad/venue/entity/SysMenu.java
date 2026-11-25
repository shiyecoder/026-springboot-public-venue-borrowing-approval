package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 菜单：按角色动态下发，菜单管理项本身对普通角色隐藏 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private Integer sortNo;
    private String roleCodes;
    private Integer hidden;
}
