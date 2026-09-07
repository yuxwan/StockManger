package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luckyun.stock.dto.UserCreateDTO;
import com.luckyun.stock.entity.Role;
import com.luckyun.stock.entity.User;
import com.luckyun.stock.entity.UserRole;
import com.luckyun.stock.mapper.RoleMapper;
import com.luckyun.stock.mapper.UserRoleMapper;
import com.luckyun.stock.service.RoleService;
import com.luckyun.stock.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@SaCheckLogin
public class SystemUserController {

    private final UserService userService;
    private final RoleService roleService;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @GetMapping("/search")
    public ResponseEntity<Page<User>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getId);
        Page<User> result = userService.page(new Page<>(page, pageSize), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<User>> list() {
        List<User> users = userService.list();
        // 不返回密码
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    /**
     * 收银员列表（收银台可切换的销售员）。
     * 规则：关联了 role.code = cashier 的角色，或 User.role 兜底字段为 cashier，
     * 且账号启用。管理员/超级管理员不参与收银。
     */
    @GetMapping("/cashiers")
    public ResponseEntity<List<User>> cashiers() {
        Set<Long> ids = new HashSet<>();

        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "cashier"));
        if (roles != null && !roles.isEmpty()) {
            List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
            userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                            .in(UserRole::getRoleId, roleIds))
                    .forEach(ur -> ids.add(ur.getUserId()));
        }
        // 兼容 User.role 字段兜底
        userService.lambdaQuery().eq(User::getRole, "cashier")
                .list().forEach(u -> ids.add(u.getId()));

        if (ids.isEmpty()) return ResponseEntity.ok(List.of());

        List<User> users = userService.listByIds(ids).stream()
                .filter(u -> u.getStatus() == null || u.getStatus() == 1)
                .collect(Collectors.toList());
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("msg", "用户不存在"));
        user.setPassword(null);
        // 返回用户的角色ID列表
        List<Long> roleIds = roleService.getUserRoles(id).stream()
                .map(r -> r.getId()).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("user", user, "roleIds", roleIds));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody UserCreateDTO dto) {
        // 检查用户名唯一性
        User exist = userService.lambdaQuery().eq(User::getUsername, dto.getUsername()).one();
        if (exist != null) return ResponseEntity.badRequest().body(Map.of("msg", "用户名已存在"));

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        // 保持向后兼容
        user.setRole("cashier");
        userService.save(user);

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            roleService.assignUserRoles(user.getId(), dto.getRoleIds());
        }

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UserCreateDTO dto) {
        User user = userService.getById(id);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("msg", "用户不存在"));

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            User exist = userService.lambdaQuery().eq(User::getUsername, dto.getUsername())
                    .ne(User::getId, id).one();
            if (exist != null) return ResponseEntity.badRequest().body(Map.of("msg", "用户名已存在"));
            user.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        userService.updateById(user);

        // 更新角色
        if (dto.getRoleIds() != null) {
            roleService.assignUserRoles(id, dto.getRoleIds());
        }

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        roleService.assignUserRoles(id, List.of());
        userService.removeById(id);
        return ResponseEntity.ok(Map.of("msg", "删除成功"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = userService.getById(id);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("msg", "用户不存在"));
        user.setStatus(body.get("status"));
        userService.updateById(user);
        return ResponseEntity.ok(Map.of("msg", "状态已更新"));
    }
}
