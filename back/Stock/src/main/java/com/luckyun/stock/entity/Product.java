package com.luckyun.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String barcode;

    private String name;

    /** 品牌规格型号 */
    private String spec;

    /** 销售价 */
    private BigDecimal price;

    /** 进货价 */
    private BigDecimal purchasePrice;

    /** 库存数量 */
    private Integer stock;

    /** 单位 */
    private String unit;

    /** 存放位置 */
    private String location;

    /** 有效期（D-天 M-月 Y-年） */
    private String expiry;

    /** 分类ID */
    private Long categoryId;

    /** 0-下架 1-上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
