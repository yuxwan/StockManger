package com.luckyun.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.Category;
import com.luckyun.stock.mapper.CategoryMapper;
import com.luckyun.stock.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
}
