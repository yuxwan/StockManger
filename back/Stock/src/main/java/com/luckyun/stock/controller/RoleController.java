package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.luckyun.stock.dto.RoleCreateDTO;
import com.luckyun.stock.entity.Role;
import com.luckyun.stock.service.MenuService;
import com.luckyun.stock.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
@SaCheckLogin
public class RoleController {

    private final RoleService roleService;
    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<List<Role>> list() {
        return ResponseEntity.ok(roleService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Role role = roleService.getById(id);
        if (role == null) return ResponseEntity.notFound().build();
        List<Long> menuIds = menuService.getRoleMenuIds(id);
        return ResponseEntity.ok(Map.of("role", role, "menuIds", menuIds));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RoleCreateDTO dto) {
        Role role = roleService.createRole(dto.getName(), dto.getCode(), dto.getRemark(), dto.getStatus(), dto.getMenuIds());
        return ResponseEntity.ok(role);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody RoleCreateDTO dto) {
        Role role = roleService.updateRole(id, dto.getName(), dto.getCode(), dto.getRemark(), dto.getStatus(), dto.getMenuIds());
        return ResponseEntity.ok(role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("msg", "删除成功"));
    }
}
