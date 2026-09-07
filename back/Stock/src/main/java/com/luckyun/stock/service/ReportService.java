package com.luckyun.stock.service;

import com.luckyun.stock.dto.ReportSummaryDTO;

import java.util.List;
import java.util.Map;

public interface ReportService {

    /** 获取报表汇总 */
    ReportSummaryDTO getSummary(String dateRange);

    /** 按员工统计销售额（分红用） */
    List<Map<String, Object>> getStaffSummary(String dateRange);
}
