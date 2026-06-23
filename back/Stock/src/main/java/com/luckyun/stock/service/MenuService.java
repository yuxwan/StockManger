package com.luckyun.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.Menu;

import java.util.List;

public interface MenuService extends IService<Menu> {
    /** 获取菜单树 */
    List<Menu> getMenuTree();

    /** 创建菜单 */
    Menu createMenu(String name, String icon, String path, String permission, Long parentId, Integer sort, Integer type);

    /** 更新菜单 */
    Menu updateMenu(Long id, String name, String icon, String path, String permission, Long parentId, Integer sort, Integer type);

    /** 删除菜单 */
    void deleteMenu(Long id);

    /** 获取角色的菜单ID列表 */
    List<Long> getRoleMenuIds(Long roleId);

    /** 获取当前登录用户的菜单树（按角色过滤） */
    List<Menu> getUserMenuTree();
}
