package com.luckyun.stock.service;

import com.luckyun.stock.dto.ReportSummaryDTO;

public interface ReportService {

    /** 获取报表汇总 */
    ReportSummaryDTO getSummary(String dateRange);
}
