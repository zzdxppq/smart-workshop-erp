-- ======================================================================
-- V94 · 员工岗位字典 EMPLOYEE_POSITION
-- 2026-06-21 · PO 范蠡
-- 目标：
--   1) crm_hr_employee.position 由「自由文本」升级为「EMPLOYEE_POSITION 字典值」
--   2) 12 条预设岗位（生产 5 + 检验 1 + 仓储 1 + 工程 1 + 销售/采购/HR/管理 4）
--   3) 部门 + 可操作设备类型存放在 remark JSON（dept / machine_types）
--   4) 后端 EmployeeService 创建/更新时校验 position ∈ EMPLOYEE_POSITION
-- ======================================================================

USE `cnc_platform`;

-- ---------- 1) 字典类型（V83 风格 · ON DUPLICATE KEY UPDATE 幂等） ----------
INSERT INTO sys_dict_type (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('EMPLOYEE_POSITION', '员工岗位', '员工岗位字典：岗位编码=POS-*；remark=JSON{dept,machine_types}', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`),
                                `description` = VALUES(`description`),
                                `is_builtin` = VALUES(`is_builtin`);

-- ---------- 2) 12 条岗位字典（INSERT IGNORE · 与 V83/V85/V89 风格一致） ----------
-- sort 1=生产核心操作工，2-5=生产辅助，6=品质，7=仓管，8=工程，9-11=业务/采购/HR，12=管理
INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status, remark, created_at, updated_at) VALUES
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

-- 注：
--   * sys_dict 既有 remark 列（V83 起即引用），本 migration 不再显式 ALTER
--   * 旧 crm_hr_employee.position（如「CNC 操作员」/「质检员」/「工艺工程师」）保留文本，
--     后端校验仅拒绝「不在字典中」的新值；后续由管理员手工对齐到 POS-* 编码
