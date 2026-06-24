package com.luckyun.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luckyun.stock.dto.ReportSummaryDTO;
import com.luckyun.stock.entity.Order;
import com.luckyun.stock.entity.OrderItem;
import com.luckyun.stock.entity.Product;
import com.luckyun.stock.mapper.OrderItemMapper;
import com.luckyun.stock.mapper.OrderMapper;
import com.luckyun.stock.mapper.ProductMapper;
import com.luckyun.stock.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Override
    public ReportSummaryDTO getSummary(String dateRange) {
        // 计算时间范围
        LocalDateTime start = getStartTime(dateRange);
        LocalDateTime end = LocalDateTime.now();

        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, start)
                .le(Order::getCreateTime, end);

        List<Order> orders = orderMapper.selectList(qw);

        // 汇总
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> "completed".equals(o.getStatus()))
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orders.stream()
                .filter(o -> "completed".equals(o.getStatus()))
                .count();

        long refundCount = orders.stream()
                .filter(o -> "refunded".equals(o.getStatus()))
                .count();

        BigDecimal refundAmount = orders.stream()
                .filter(o -> "refunded".equals(o.getStatus()))
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), RoundingMode.HALF_UP).intValue()
                : 0;

        // 收入趋势（按天聚合）
        Map<String, BigDecimal> trendMap = new LinkedHashMap<>();
        orders.stream().filter(o -> "completed".equals(o.getStatus())).forEach(o -> {
            String day = o.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            trendMap.merge(day, o.getTotal(), BigDecimal::add);
        });
        List<Map<String, Object>> revenueTrend = trendMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("date", e.getKey());
                    m.put("revenue", e.getValue());
                    return m;
                }).collect(Collectors.toList());

        // 支付方式分布
        Map<String, BigDecimal> payMap = new HashMap<>();
        orders.stream().filter(o -> "completed".equals(o.getStatus())).forEach(o -> {
            String label = switch (o.getPayment()) {
                case "wechat" -> "微信支付";
                case "alipay" -> "支付宝";
                case "cash" -> "现金";
                default -> o.getPayment();
            };
            payMap.merge(label, o.getTotal(), BigDecimal::add);
        });
        List<Map<String, Object>> paymentBreakdown = payMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                }).collect(Collectors.toList());

        // 热销商品（按时间范围过滤）
        List<Long> orderIds = orders.stream()
                .filter(o -> "completed".equals(o.getStatus()))
                .map(Order::getId)
                .collect(Collectors.toList());

        List<Map<String, Object>> topProducts = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<OrderItem> itemQw = new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds);
            List<OrderItem> allItems = orderItemMapper.selectList(itemQw);
            Map<String, long[]> productSalesMap = new HashMap<>();
            for (OrderItem item : allItems) {
                productSalesMap.compute(item.getProductName(), (k, v) -> {
                    if (v == null) v = new long[2];
                    v[0] += item.getQuantity();
                    v[1] += item.getSubtotal().longValue();
                    return v;
                });
            }
            topProducts = productSalesMap.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                    .limit(10)
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", e.getKey());
                        m.put("qty", e.getValue()[0]);
                        m.put("revenue", e.getValue()[1]);
                        return m;
                    }).collect(Collectors.toList());
        }

        // 分类汇总（简化：全部归为"商品"）
        List<Map<String, Object>> categorySummary = List.of(
                Map.of("name", "商品", "value", totalRevenue)
        );

        ReportSummaryDTO dto = new ReportSummaryDTO();
        dto.setTotalRevenue(totalRevenue);
        dto.setTotalOrders((int) totalOrders);
        dto.setAvgOrderValue(avgOrderValue);
        dto.setRefundCount((int) refundCount);
        dto.setRefundAmount(refundAmount);
        dto.setRevenueTrend(revenueTrend);
        dto.setPaymentBreakdown(paymentBreakdown);
        dto.setTopProducts(topProducts);
        dto.setCategorySummary(categorySummary);

        // 低库存商品（库存 < 10）
        List<Product> lowStock = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .lt(Product::getStock, 10)
                        .orderByAsc(Product::getStock)
        );
        List<Map<String, Object>> lowStockProducts = lowStock.stream()
                .filter(p -> p.getStock() > 0)
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", p.getName());
                    m.put("stock", p.getStock());
                    m.put("unit", p.getUnit() != null ? p.getUnit() : "件");
                    m.put("id", p.getId());
                    m.put("barcode", p.getBarcode());
                    return m;
                }).collect(Collectors.toList());
        dto.setLowStockProducts(lowStockProducts);

        return dto;
    }

    private LocalDateTime getStartTime(String dateRange) {
        LocalDate now = LocalDate.now();
        return switch (dateRange) {
            case "today" -> now.atStartOfDay();
            case "week" -> now.minusDays(7).atStartOfDay();
            case "month" -> now.minusMonths(1).atStartOfDay();
            case "year" -> now.withDayOfYear(1).atStartOfDay();
            default -> now.minusDays(7).atStartOfDay();
        };
    }
}
