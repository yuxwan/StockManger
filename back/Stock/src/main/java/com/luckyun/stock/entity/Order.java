package com.luckyun.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pos_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 订单总金额 */
    private BigDecimal total;

    /** 折扣百分比（如 10 表示 10% off） */
    private BigDecimal discount;

    /** 商品种数 */
    private Integer itemCount;

    /** wechat / alipay / cash */
    private String payment;

    /** completed / refunded */
    private String status;

    /** 备注 */
    private String remark;

    /** 操作员ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
