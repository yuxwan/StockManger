package com.luckyun.stock.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.Role;
import com.luckyun.stock.entity.User;
import com.luckyun.stock.mapper.UserMapper;
import com.luckyun.stock.service.RoleService;
import com.luckyun.stock.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final RoleService roleService;

    private String getUserRoleCode(Long userId) {
        List<Role> roles = roleService.getUserRoles(userId);
        if (roles != null && !roles.isEmpty()) {
            for (Role role : roles) {
                if ("admin".equals(role.getCode()) || "super_admin".equals(role.getCode())) {
                    return "admin";
                }
            }
            return roles.get(0).getCode();
        }
        return null;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        String role = getUserRoleCode(user.getId());
        result.put("role", role != null ? role : (user.getRole() != null ? user.getRole() : "cashier"));
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return result;
    }

    @Override
    public void changePassword(long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        updateById(user);
    }
}
