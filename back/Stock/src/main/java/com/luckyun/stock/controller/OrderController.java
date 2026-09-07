package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luckyun.stock.dto.OrderCreateDTO;
import com.luckyun.stock.entity.Order;
import com.luckyun.stock.entity.OrderItem;
import com.luckyun.stock.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SaCheckLogin
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/search")
    public ResponseEntity<Page<Order>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Order::getOrderNo, keyword);
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
        orderService.refundOrder(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/items/{itemId}/refund")
    public ResponseEntity<Void> refundItem(@PathVariable Long orderId, @PathVariable Long itemId, @RequestBody Map<String, Integer> body) {
        orderService.refundOrderItem(orderId, itemId, body.getOrDefault("quantity", 1));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
