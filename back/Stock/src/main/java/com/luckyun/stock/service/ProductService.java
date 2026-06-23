package com.luckyun.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.Product;

public interface ProductService extends IService<Product> {

    /** 创建商品，若条码重复抛异常 */
    Product createProduct(Product product);
}
