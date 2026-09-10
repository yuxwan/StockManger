package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luckyun.stock.entity.OperationLog;
import com.luckyun.stock.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@SaCheckLogin
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 操作日志列表
     * 传 page/pageSize 走分页（移动端上拉加载）；传 limit 走一次拉取（桌面端）
     * types 可选，逗号分隔的日志类型过滤
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) String types,
            @RequestParam(defaultValue = "200") int limit) {
        List<String> typeList = null;
        if (types != null && !types.isBlank()) {
            typeList = Arrays.asList(types.split(","));
        }
        if (page != null && pageSize != null) {
            IPage<OperationLog> result = operationLogService.getPage(page, pageSize, typeList);
            return ResponseEntity.ok(result);
        }
        List<OperationLog> list = operationLogService.getRecent(limit);
        return ResponseEntity.ok(list);
    }
}
