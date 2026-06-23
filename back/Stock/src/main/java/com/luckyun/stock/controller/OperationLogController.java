package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.luckyun.stock.entity.OperationLog;
import com.luckyun.stock.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@SaCheckLogin
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public ResponseEntity<List<OperationLog>> list(@RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(operationLogService.getRecent(limit));
    }
}
