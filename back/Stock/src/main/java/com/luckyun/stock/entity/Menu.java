package com.luckyun.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String icon;

    private String path;

    /** 权限标识 */
    private String permission;

    /** 父菜单ID */
    private Long parentId;

    /** 排序 */
    private Integer sort;

    /** 1-目录 2-菜单 3-按钮/权限 */
    private Integer type;

    @TableField(exist = false)
    private List<Menu> children;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
