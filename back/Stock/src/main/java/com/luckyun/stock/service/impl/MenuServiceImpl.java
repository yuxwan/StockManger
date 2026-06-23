package com.luckyun.stock.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.Menu;
import com.luckyun.stock.entity.RoleMenu;
import com.luckyun.stock.entity.UserRole;
import com.luckyun.stock.mapper.MenuMapper;
import com.luckyun.stock.mapper.RoleMenuMapper;
import com.luckyun.stock.mapper.UserRoleMapper;
import com.luckyun.stock.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<Menu> getUserMenuTree() {
        long userId = StpUtil.getLoginIdAsLong();
        // 获取当前用户的所有角色
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());
        // 获取所有角色的菜单 ID（去重）
        Set<Long> menuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds)
        ).stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
        // admin 角色拥有所有菜单
        if (roleIds.isEmpty()) {
            List<Menu> all = list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
            return buildTree(all, 0L);
        }
        List<Menu> myMenus = list(new LambdaQueryWrapper<Menu>()
                .in(Menu::getId, menuIds)
                .orderByAsc(Menu::getSort));
        return buildTree(myMenus, 0L);
    }

    @Override
    public List<Menu> getMenuTree() {
        List<Menu> all = list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
        return buildTree(all, 0L);
    }

    private List<Menu> buildTree(List<Menu> all, Long parentId) {
        return all.stream()
                .filter(m -> (m.getParentId() == null ? Long.valueOf(0L) : m.getParentId()).equals(parentId))
                .peek(m -> m.setChildren(buildTree(all, m.getId())))
                .sorted(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0))
                .collect(Collectors.toList());
    }

    @Override
    public Menu createMenu(String name, String icon, String path, String permission, Long parentId, Integer sort, Integer type) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setIcon(icon);
        menu.setPath(path);
        menu.setPermission(permission);
        menu.setParentId(parentId != null ? parentId : 0L);
        menu.setSort(sort != null ? sort : 0);
        menu.setType(type != null ? type : 2);
        save(menu);
        return menu;
    }

    @Override
    public Menu updateMenu(Long id, String name, String icon, String path, String permission, Long parentId, Integer sort, Integer type) {
        Menu menu = getById(id);
        if (menu == null) throw new RuntimeException("菜单不存在");
        menu.setName(name);
        menu.setIcon(icon);
        menu.setPath(path);
        menu.setPermission(permission);
        menu.setParentId(parentId != null ? parentId : 0L);
        menu.setSort(sort != null ? sort : 0);
        menu.setType(type != null ? type : menu.getType());
        updateById(menu);
        return menu;
    }

    @Override
    public void deleteMenu(Long id) {
        // 删除子菜单
        remove(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        // 删除关联
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id));
        // 删除自身
        removeById(id);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        ).stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }
}
