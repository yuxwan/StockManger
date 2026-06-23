package com.luckyun.stock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RoleCreateDTO {
    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    private String remark;

    private Integer status;

    /** 关联的菜单ID列表 */
    private List<Long> menuIds;
}
