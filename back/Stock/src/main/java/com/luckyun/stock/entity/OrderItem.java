package com.luckyun.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 单价（快照） */
    private BigDecimal price;

    /** 改价前原价（快照，临时特价时才有值） */
    private BigDecimal originalPrice;

    /** 数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal subtotal;

    /** 已退款数量 */
    private Integer refundedQty;
}
