package com.luckyun.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    /** 创建角色 */
    Role createRole(String name, String code, String remark, Integer status, List<Long> menuIds);

    /** 更新角色 */
    Role updateRole(Long id, String name, String code, String remark, Integer status, List<Long> menuIds);

    /** 删除角色 */
    void deleteRole(Long id);

    /** 获取用户角色列表 */
    List<Role> getUserRoles(Long userId);

    /** 为用户分配角色 */
    void assignUserRoles(Long userId, List<Long> roleIds);
}
