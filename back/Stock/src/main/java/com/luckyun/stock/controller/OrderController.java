package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.luckyun.stock.dto.OrderCreateDTO;
import com.luckyun.stock.entity.Order;
import com.luckyun.stock.entity.OrderItem;
import com.luckyun.stock.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        long userId = StpUtil.getLoginIdAsLong();
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
