package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luckyun.stock.dto.OrderCreateDTO;
import com.luckyun.stock.entity.Order;
import com.luckyun.stock.entity.OrderItem;
import com.luckyun.stock.service.OperationLogService;
import com.luckyun.stock.service.OrderService;
import com.luckyun.stock.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SaCheckLogin
public class OrderController {

    private final OrderService orderService;
    private final OperationLogService operationLogService;
    private final UserService userService;

    /** 当前操作人显示名（与 ProductController 口径一致） */
    private String getOperatorName() {
        long userId = StpUtil.getLoginIdAsLong();
        var user = userService.getById(userId);
        return user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "未知";
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Order>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        // 按卖出人（下单操作员）筛选
        if (sellerId != null) {
            wrapper.eq(Order::getUserId, sellerId);
        }
        // 时间范围（格式 yyyy-MM-dd HH:mm:ss）
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(Order::getCreateTime, LocalDateTime.parse(startTime, fmt));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(Order::getCreateTime, LocalDateTime.parse(endTime, fmt));
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return ResponseEntity.ok(orderService.page(new Page<>(page, pageSize), wrapper));
    }

    @GetMapping
    public ResponseEntity<List<Order>> list() {
        return ResponseEntity.ok(orderService.lambdaQuery()
                .orderByDesc(Order::getCreateTime)
                .list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        List<OrderItem> items = orderService.getItemsByOrderId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderCreateDTO dto) {
        // 若指定了销售员则用指定的人，否则归属当前登录人
        long userId = dto.getSaleByUserId() != null
                ? dto.getSaleByUserId()
                : StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(orderService.createOrder(dto, userId));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Void> refund(@PathVariable Long id) {
        Order order = orderService.getById(id);
        orderService.refundOrder(id);
        if (order != null) {
            operationLogService.log("REFUND_ORDER", "ORDER", id, order.getOrderNo(),
                    "整单退款（金额：¥" + order.getTotal() + "）", getOperatorName());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/items/{itemId}/refund")
    public ResponseEntity<Void> refundItem(@PathVariable Long orderId, @PathVariable Long itemId, @RequestBody Map<String, Integer> body) {
        int quantity = body.getOrDefault("quantity", 1);
        Order order = orderService.getById(orderId);
        String itemName = null;
        if (order != null && quantity > 0) {
            itemName = orderService.getItemsByOrderId(orderId).stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst().map(OrderItem::getProductName).orElse(null);
        }
        orderService.refundOrderItem(orderId, itemId, quantity);
        if (order != null) {
            operationLogService.log("REFUND_ITEM", "ORDER", orderId, order.getOrderNo(),
                    "单品退款" + (itemName != null ? "：" + itemName + " x" + quantity : "（明细id=" + itemId + "）x" + quantity),
                    getOperatorName());
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Order order = orderService.getById(id);
        orderService.deleteOrder(id);
        if (order != null) {
            operationLogService.log("DELETE_ORDER", "ORDER", id, order.getOrderNo(),
                    "删除订单（金额：¥" + order.getTotal() + "，商品库存已恢复）", getOperatorName());
        }
        return ResponseEntity.noContent().build();
    }
}
