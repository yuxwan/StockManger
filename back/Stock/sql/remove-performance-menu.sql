-- 移除"绩效管理/员工业绩"菜单（员工绩效功能已下线，改为在订单中查看创建人自行统计）
-- 说明：
--   1. 幂等，可重复执行；
--   2. sys_role_menu.menu_id 有 ON DELETE CASCADE，会随菜单记录一并删除，无需手动清理角色关联。
DELETE FROM sys_menu WHERE permission = 'performance' OR path = '/performance';
