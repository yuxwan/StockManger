package com.luckyun.stock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuCreateDTO {
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    private String icon;

    private String path;

    private String permission;

    private Long parentId;

    private Integer sort;

    /** 1-目录 2-菜单 3-按钮/权限 */
    private Integer type;
}
