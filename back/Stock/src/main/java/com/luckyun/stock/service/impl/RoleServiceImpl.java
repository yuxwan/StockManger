package com.luckyun.stock.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckyun.stock.entity.Role;
import com.luckyun.stock.entity.RoleMenu;
import com.luckyun.stock.entity.UserRole;
import com.luckyun.stock.mapper.RoleMapper;
import com.luckyun.stock.mapper.RoleMenuMapper;
import com.luckyun.stock.mapper.UserRoleMapper;
import com.luckyun.stock.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional
    public Role createRole(String name, String code, String remark, Integer status, List<Long> menuIds) {
        // 检查编码唯一性
        Role exist = lambdaQuery().eq(Role::getCode, code).one();
        if (exist != null) throw new RuntimeException("角色编码已存在");

        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setRemark(remark);
        role.setStatus(status != null ? status : 1);
        save(role);

        if (menuIds != null && !menuIds.isEmpty()) {
            assignMenuPermissions(role.getId(), menuIds);
        }
        return role;
    }

    @Override
    @Transactional
    public Role updateRole(Long id, String name, String code, String remark, Integer status, List<Long> menuIds) {
        Role role = getById(id);
        if (role == null) throw new RuntimeException("角色不存在");

        role.setName(name);
        role.setCode(code);
        role.setRemark(remark);
        role.setStatus(status != null ? status : 1);
        updateById(role);

        // 更新菜单权限
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        if (menuIds != null && !menuIds.isEmpty()) {
            assignMenuPermissions(id, menuIds);
        }
        return role;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        removeById(id);
    }

    @Override
    public List<Role> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());
        return roleIds.isEmpty() ? List.of() : listByIds(roleIds);
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    private void assignMenuPermissions(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }
}
