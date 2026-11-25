package com.grad.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 字典：阶段、状态、作业类型等下拉项，禁止前端提交未定义值 */
@Data
@TableName("sys_dict")
public class SysDict {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sortNo;
    private Integer status;
}
