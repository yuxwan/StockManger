package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.luckyun.stock.entity.Category;
import com.luckyun.stock.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@SaCheckLogin
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> list() {
        return ResponseEntity.ok(categoryService.lambdaQuery()
                .orderByAsc(Category::getSort)
                .list());
    }

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody Category category) {
        categoryService.save(category);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return ResponseEntity.ok().build();
    }
}
