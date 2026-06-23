package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.luckyun.stock.dto.ReportSummaryDTO;
import com.luckyun.stock.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SaCheckLogin
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDTO> summary(@RequestParam(defaultValue = "week") String dateRange) {
        return ResponseEntity.ok(reportService.getSummary(dateRange));
    }
}
