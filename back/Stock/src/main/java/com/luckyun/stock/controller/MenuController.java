package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.luckyun.stock.dto.MenuCreateDTO;
import com.luckyun.stock.entity.Menu;
import com.luckyun.stock.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
@SaCheckLogin
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/my")
    public ResponseEntity<List<Menu>> myMenus() {
        return ResponseEntity.ok(menuService.getUserMenuTree());
    }

    @GetMapping("/tree")
    public ResponseEntity<List<Menu>> tree() {
        return ResponseEntity.ok(menuService.getMenuTree());
    }

    @GetMapping
    public ResponseEntity<List<Menu>> list() {
        return ResponseEntity.ok(menuService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Menu menu = menuService.getById(id);
        if (menu == null) return ResponseEntity.badRequest().body(Map.of("msg", "菜单不存在"));
        return ResponseEntity.ok(menu);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MenuCreateDTO dto) {
        Menu menu = menuService.createMenu(
                dto.getName(), dto.getIcon(), dto.getPath(), dto.getPermission(),
                dto.getParentId(), dto.getSort(), dto.getType()
        );
        return ResponseEntity.ok(menu);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody MenuCreateDTO dto) {
        Menu menu = menuService.updateMenu(
                id, dto.getName(), dto.getIcon(), dto.getPath(), dto.getPermission(),
                dto.getParentId(), dto.getSort(), dto.getType()
        );
        return ResponseEntity.ok(menu);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok(Map.of("msg", "删除成功"));
    }
}
