package com.luckyun.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ReportSummaryDTO {
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private Integer avgOrderValue;
    private Integer refundCount;
    private BigDecimal refundAmount;
    private List<Map<String, Object>> revenueTrend;
    private List<Map<String, Object>> paymentBreakdown;
    private List<Map<String, Object>> topProducts;
    private List<Map<String, Object>> categorySummary;
    private List<Map<String, Object>> lowStockProducts;
}
