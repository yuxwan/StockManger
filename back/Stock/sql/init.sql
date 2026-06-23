-- Stock POS 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS stock DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE stock;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    role VARCHAR(20) DEFAULT 'cashier' COMMENT 'admin / cashier',
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode VARCHAR(50) UNIQUE,
    name VARCHAR(200) NOT NULL,
    spec VARCHAR(200) DEFAULT '' COMMENT '品牌规格型号',
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    purchase_price DECIMAL(10,2) DEFAULT 0 COMMENT '进货价',
    stock INT NOT NULL DEFAULT 0,
    unit VARCHAR(20) DEFAULT '个',
    location VARCHAR(100),
    expiry VARCHAR(50) COMMENT 'D-天 M-月 Y-年',
    category_id BIGINT,
    status INT DEFAULT 1 COMMENT '0-下架 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS pos_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    item_count INT NOT NULL DEFAULT 0,
    payment VARCHAR(20) COMMENT 'wechat / alipay / cash',
    status VARCHAR(20) DEFAULT 'completed' COMMENT 'completed / refunded',
    remark VARCHAR(255),
    user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(200),
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    quantity INT NOT NULL DEFAULT 1,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (order_id) REFERENCES pos_order(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    target_name VARCHAR(200) COMMENT '目标名称',
    detail VARCHAR(500) COMMENT '操作描述',
    operator_name VARCHAR(50) COMMENT '操作员',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    icon VARCHAR(50) COMMENT '图标',
    path VARCHAR(200) COMMENT '路由路径',
    permission VARCHAR(100) COMMENT '权限标识',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    sort INT DEFAULT 0 COMMENT '排序',
    type INT DEFAULT 1 COMMENT '1-目录 2-菜单 3-按钮/权限',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    remark VARCHAR(255) COMMENT '备注',
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认菜单（基础数据）
INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type) VALUES
('收银', 'mdi:cart-outline', '/checkout', 'checkout', 0, 1, 2),
('仪表盘', 'mdi:view-dashboard-outline', '/dashboard', 'dashboard', 0, 2, 2),
('商品管理', 'mdi:package-variant-closed', '/products', 'products', 0, 3, 2),
('订单管理', 'mdi:receipt-text-outline', '/orders', 'orders', 0, 4, 2),
('操作日志', 'mdi:clipboard-text-clock-outline', '/operations', 'operations', 0, 5, 2),
('系统管理', 'mdi:cog-outline', '', 'system', 0, 6, 1);

SET @sid = (SELECT id FROM sys_menu WHERE permission = 'system' LIMIT 1);
INSERT INTO sys_menu (name, icon, path, permission, parent_id, sort, type) VALUES
('用户管理', 'mdi:account-group-outline', '/users', 'system:users', @sid, 1, 2),
('角色管理', 'mdi:shield-account-outline', '/roles', 'system:roles', @sid, 2, 2),
('菜单管理', 'mdi:menu-open', '/menus', 'system:menus', @sid, 3, 2);

-- 默认角色
INSERT INTO sys_role (name, code, remark) VALUES
('超级管理员', 'admin', '系统超级管理员，拥有所有权限'),
('收银员', 'cashier', '普通收银员');

-- 默认角色-菜单关联（admin 拥有所有菜单权限，cashier 只拥有业务菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m WHERE r.code = 'admin';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code = 'cashier' AND m.permission IN ('checkout', 'dashboard', 'products', 'orders', 'operations');

-- 订单表加折扣字段
ALTER TABLE pos_order ADD COLUMN IF NOT EXISTS discount DECIMAL(5,2) DEFAULT NULL COMMENT '折扣百分比';
-- 订单明细加已退数量字段
ALTER TABLE order_item ADD COLUMN refunded_qty INT DEFAULT 0 COMMENT '已退款数量';

-- 默认管理员关联 admin 角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'admin';

-- 默认管理员（密码: admin123）
INSERT IGNORE INTO sys_user (username, password, nickname, role) VALUES
('admin', '$2b$10$MponB3pVabsCK8omicxilONePSGlQB2nj.d7FA33ZWygT48zctA8O', '管理员', 'admin');
