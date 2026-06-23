package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.luckyun.stock.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SaCheckLogin
public class UserController {

    private final UserService userService;

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("msg", "请输入原密码"));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("msg", "新密码不能少于6位"));
        }
        long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("msg", "修改成功"));
    }
}
