package com.luckyun.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.Order;
import com.luckyun.stock.entity.OrderItem;
import com.luckyun.stock.dto.OrderCreateDTO;

import java.util.List;

public interface OrderService extends IService<Order> {

    /** 创建订单 */
    Order createOrder(OrderCreateDTO dto, Long userId);

    /** 获取订单商品明细 */
    List<OrderItem> getItemsByOrderId(Long orderId);

    /** 删除订单及其商品明细 */
    void deleteOrder(Long id);

    /** 退款：恢复库存并标记状态为 refunded */
    void refundOrder(Long id);

    /** 单品退款 */
    void refundOrderItem(Long orderId, Long itemId, Integer quantity);
}
