package com.luckyun.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.Order;
import com.luckyun.stock.entity.OrderItem;
import com.luckyun.stock.entity.Product;
import com.luckyun.stock.mapper.OrderItemMapper;
import com.luckyun.stock.mapper.OrderMapper;
import com.luckyun.stock.mapper.ProductMapper;
import com.luckyun.stock.service.OrderService;
import com.luckyun.stock.dto.OrderCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderCreateDTO dto, Long userId) {
        // 计算原价总金额
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = dto.getItems().size();
        for (OrderCreateDTO.Item item : dto.getItems()) {
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
        }

        // 应用折扣（实收百分比，如 80 表示打八折，收 80%）
        BigDecimal finalTotal = total;
        if (dto.getDiscount() != null && dto.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal payRate = dto.getDiscount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            finalTotal = total.multiply(payRate).setScale(2, RoundingMode.HALF_UP);
        }

        // 生成订单编号
        String orderNo = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", ORDER_SEQ.incrementAndGet() % 1000);

        // 保存订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setTotal(finalTotal);
        order.setDiscount(dto.getDiscount());
        order.setItemCount(itemCount);
        order.setPayment(dto.getPayment());
        order.setStatus("completed");
        order.setRemark(dto.getRemark());
        order.setUserId(userId);
        save(order);

        // 保存订单明细 + 扣减库存
        for (OrderCreateDTO.Item item : dto.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(item.getProductId());
            oi.setProductName(item.getProductName());
            oi.setPrice(item.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);

            // 扣减库存
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() - item.getQuantity());
                productMapper.updateById(product);
            }
        }

        return order;
    }

    @Override
    public List<OrderItem> getItemsByOrderId(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long id) {
        Order order = getById(id);
        if (order == null) return;
        if ("refunded".equals(order.getStatus())) return;

        // 恢复库存 & 标记单品已退
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
        );
        for (OrderItem item : items) {
            if (item.getProductId() != null) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    int refundable = item.getQuantity() - (item.getRefundedQty() != null ? item.getRefundedQty() : 0);
                    if (refundable > 0) {
                        product.setStock(product.getStock() + refundable);
                        productMapper.updateById(product);
                    }
                }
            }
            item.setRefundedQty(item.getQuantity());
            orderItemMapper.updateById(item);
        }

        order.setStatus("refunded");
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrderItem(Long orderId, Long itemId, Integer quantity) {
        Order order = getById(orderId);
        if (order == null) return;
        if ("refunded".equals(order.getStatus())) return; // 整单已退不能再单品退

        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !item.getOrderId().equals(orderId)) return;

        int refunded = item.getRefundedQty() != null ? item.getRefundedQty() : 0;
        int refundable = item.getQuantity() - refunded;
        if (quantity <= 0 || quantity > refundable) return;

        // 恢复库存
        if (item.getProductId() != null) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + quantity);
                productMapper.updateById(product);
            }
        }

        // 更新已退数量
        item.setRefundedQty(refunded + quantity);
        orderItemMapper.updateById(item);

        // 检查是否全部退完了
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        boolean allRefunded = allItems.stream().allMatch(
                i -> (i.getRefundedQty() != null ? i.getRefundedQty() : 0) >= i.getQuantity()
        );
        if (allRefunded) {
            order.setStatus("refunded");
            updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        // 先获取订单商品明细，恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
        );
        for (OrderItem item : items) {
            if (item.getProductId() != null) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productMapper.updateById(product);
                }
            }
        }
        // 删除订单明细
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        // 删除订单
        removeById(id);
    }
}
