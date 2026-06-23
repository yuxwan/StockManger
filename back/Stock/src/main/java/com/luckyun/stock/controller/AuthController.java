package com.luckyun.stock.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.luckyun.stock.dto.LoginDTO;
import com.luckyun.stock.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto) {
        Map<String, Object> result = userService.login(dto.getUsername(), dto.getPassword());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/info")
    public ResponseEntity<?> info() {
        long userId = StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        StpUtil.logout();
        return ResponseEntity.ok(Map.of("msg", "ok"));
    }
}
