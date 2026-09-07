package com.luckyun.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateDTO {

    /** wechat / alipay / cash */
    @NotBlank
    private String payment;

    /** 折扣百分比（如 80 表示打八折，收 80%），可为 null */
    private BigDecimal discount;

    /** 实际销售员ID（可选，默认取当前登录人） */
    private Long saleByUserId;

    private String remark;

    @NotNull
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long productId;

        @NotBlank
        private String productName;

        @NotNull
        private BigDecimal price;

        /** 改价前原价（可选，用于标记临时特价） */
        private BigDecimal originalPrice;

        @NotNull
        private Integer quantity;
    }
}
