-- V2.1 终局：员工岗位字典 + 菜单全量重建 + 角色权限（init.sql 末尾执行，覆盖旧菜单种子）
-- 与 migrations/V94__*.sql 保持同步；改此处后请运行 tools/merge-init.py

USE `cnc_platform`;

INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('EMPLOYEE_POSITION', '员工岗位', '员工岗位字典：岗位编码=POS-*；remark=JSON{dept,machine_types}', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`),
                                `description` = VALUES(`description`),
                                `is_builtin` = VALUES(`is_builtin`);

INSERT IGNORE INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`, `remark`, `created_at`, `updated_at`) VALUES
('EMPLOYEE_POSITION', 'POS-CNC',       'CNC 操作工',    1,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','CNC加工中心'),             NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-LATHE',     '车床操作工',    2,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','数控车床'),               NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-MILL',      '铣床操作工',    3,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','铣床'),                   NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-GRIND',     '磨床操作工',    4,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','精密磨床'),               NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-EDM',       '电火花操作工',  5,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','电火花'),                 NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-QC',        '品质检验员',    6,  'ACTIVE', JSON_OBJECT('dept','品质部','machine_types','三次元/测量设备'),         NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-WAREHOUSE', '仓管员',        7,  'ACTIVE', JSON_OBJECT('dept','仓储部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-ENG',       '工程师',        8,  'ACTIVE', JSON_OBJECT('dept','工程部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-SALES',     '业务员',        9,  'ACTIVE', JSON_OBJECT('dept','销售部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-PURCHASE',  '采购员',       10,  'ACTIVE', JSON_OBJECT('dept','采购部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-HR',        '人事专员',     11,  'ACTIVE', JSON_OBJECT('dept','人事部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-MGR',       '管理人员',     12,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types',''),                       NOW(), NOW());

DELETE FROM `sys_role_permission`;
DELETE FROM `sys_menu`;

-- 以下菜单/权限块与 init.sql V2.1 段一致，维护时请同步更新 install 与 migrations/V94__v21_menu_role_permission.sql
