-- =====================================================================
-- 商品管理按钮权限迁移（针对已运行的库，可重复执行）
--
-- 背景：商品管理页的「新增/编辑/删除/入库/出库/打印标签」按钮改为由
--       菜单(type=3 按钮/权限)控制显示，后端接口同步校验对应权限。
--
-- 执行方式：
--   mysql -u<user> -p <stock_db> < alter-product-button-permissions.sql
--
-- 执行后请到「系统管理 → 角色管理」为需要操作商品的角色勾选相应按钮权限。
-- =====================================================================

-- 1. 在「商品管理」菜单下新增 6 个按钮权限节点（type=3）
INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type)
SELECT '新增商品', '', '', 'products:add', m.id, 1, 3 FROM sys_menu m
WHERE m.permission = 'products'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'products:add');

INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type)
SELECT '编辑商品', '', '', 'products:edit', m.id, 2, 3 FROM sys_menu m
WHERE m.permission = 'products'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'products:edit');

INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type)
SELECT '删除商品', '', '', 'products:delete', m.id, 3, 3 FROM sys_menu m
WHERE m.permission = 'products'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'products:delete');

INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type)
SELECT '商品入库', '', '', 'products:stock-in', m.id, 4, 3 FROM sys_menu m
WHERE m.permission = 'products'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'products:stock-in');

INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type)
SELECT '商品出库', '', '', 'products:stock-out', m.id, 5, 3 FROM sys_menu m
WHERE m.permission = 'products'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'products:stock-out');

INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type)
SELECT '打印标签', '', '', 'products:print', m.id, 6, 3 FROM sys_menu m
WHERE m.permission = 'products'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'products:print');

-- 2. 将商品按钮权限授权给超管角色（admin / super_admin），若这些角色存在
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code IN ('admin', 'super_admin')
  AND m.permission IN ('products:add', 'products:edit', 'products:delete', 'products:stock-in', 'products:stock-out', 'products:print')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
