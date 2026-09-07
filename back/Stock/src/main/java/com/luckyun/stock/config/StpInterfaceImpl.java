package com.luckyun.stock.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luckyun.stock.entity.Menu;
import com.luckyun.stock.entity.Role;
import com.luckyun.stock.entity.RoleMenu;
import com.luckyun.stock.entity.UserRole;
import com.luckyun.stock.mapper.MenuMapper;
import com.luckyun.stock.mapper.RoleMapper;
import com.luckyun.stock.mapper.RoleMenuMapper;
import com.luckyun.stock.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限数据源：当前用户的权限标识来自其角色在菜单中配置的 permission。
 * 与 MenuServiceImpl#getUserMenuTree 的数据口径保持一致：
 * - 角色包含 admin / super_admin 视为超管，拥有全部权限（"*"）
 * - 无任何角色时与菜单逻辑一致，视为拥有全部菜单
 * - 其余按 sys_role_menu 关联的菜单 permission 汇总
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        long userId = Long.parseLong(String.valueOf(loginId));

        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).distinct().collect(Collectors.toList());

        // 无角色：与菜单树逻辑一致，拥有全部
        if (roleIds.isEmpty()) {
            return List.of("*");
        }

        // 超管角色放行全部
        List<String> roleCodes = roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getCode).collect(Collectors.toList());
        if (roleCodes.contains("admin") || roleCodes.contains("super_admin")) {
            return List.of("*");
        }

        // 普通角色：汇总其被分配菜单（含目录/菜单/按钮）的权限标识
        List<Long> menuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds)
        ).stream().map(RoleMenu::getMenuId).distinct().collect(Collectors.toList());
        if (menuIds.isEmpty()) return List.of();

        return menuMapper.selectBatchIds(menuIds).stream()
                .map(Menu::getPermission)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
