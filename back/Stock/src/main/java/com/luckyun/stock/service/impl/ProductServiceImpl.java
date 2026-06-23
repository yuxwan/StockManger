package com.luckyun.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.Product;
import com.luckyun.stock.mapper.ProductMapper;
import com.luckyun.stock.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public Product createProduct(Product product) {
        save(product);
        return product;
    }
}
