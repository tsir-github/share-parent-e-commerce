-- ============================================================
-- 商家端菜单 + 角色初始化（share-system 库）
-- Menu IDs: 2071-2080（自增预留）
-- ============================================================

-- 1. 商家管理目录（顶级菜单）
INSERT INTO `share-system`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2071, '商家管理', 0, 7, 'merchant', NULL, 1, 0, 'M', '0', '0', '', 'merchant', 'admin', NOW(), 'admin', NOW(), '商家管理目录');

-- 2. 商家控制台
INSERT INTO `share-system`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2072, '控制台', 2071, 1, 'dashboard', 'merchant/dashboard/index', 1, 0, 'C', '0', '0', 'merchant:dashboard:view', 'dashboard', 'admin', NOW(), 'admin', NOW(), '商家控制台');

-- 3. 商品管理
INSERT INTO `share-system`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2073, '商品管理', 2071, 2, 'products', 'merchant/product/index', 1, 0, 'C', '0', '0', 'merchant:product:list', 'goods', 'admin', NOW(), 'admin', NOW(), '商家商品管理');

-- 4. 订单管理
INSERT INTO `share-system`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2074, '订单管理', 2071, 3, 'orders', 'merchant/order/index', 1, 0, 'C', '0', '0', 'merchant:order:list', 'order', 'admin', NOW(), 'admin', NOW(), '商家订单管理');

-- 5. 售后管理
INSERT INTO `share-system`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2075, '售后管理', 2071, 4, 'after-sale', 'merchant/afterSale/index', 1, 0, 'C', '0', '0', 'merchant:after-sale:list', 'form', 'admin', NOW(), 'admin', NOW(), '商家售后管理');

-- 6. 店铺设置
INSERT INTO `share-system`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2076, '店铺设置', 2071, 5, 'settings', 'merchant/setting/index', 1, 0, 'C', '0', '0', 'merchant:setting:view', 'setting', 'admin', NOW(), 'admin', NOW(), '商家店铺设置');

-- 7. 商家角色
INSERT INTO `share-system`.sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (101, '商家用户', 'merchant', 5, '1', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '商家用户角色');

-- 8. 角色-菜单关联（商家角色只能看到商家管理的菜单）
INSERT INTO `share-system`.sys_role_menu (role_id, menu_id)
SELECT 101, menu_id FROM `share-system`.sys_menu WHERE menu_id IN (2071, 2072, 2073, 2074, 2075, 2076);
