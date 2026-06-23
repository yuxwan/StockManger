package com.luckyun.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度2-50位")
    private String username;

    @Size(min = 6, message = "密码不能少于6位")
    private String password;

    private String nickname;

    private List<Long> roleIds;

    /** 0-禁用 1-启用 */
    private Integer status;
}
