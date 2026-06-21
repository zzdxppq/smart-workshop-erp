-- V110 · 工序-成本项自动匹配规则（V1.3.9 CNC工艺库报价联动）
USE `cnc_business`;

-- mdm_process 表结构升级（支持中文 machine_type + comment 字段）
SET @col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mdm_process'
      AND COLUMN_NAME = 'comment'
);
SET @sql = IF(@col = 0,
    'ALTER TABLE `mdm_process` ADD COLUMN `comment` VARCHAR(200) DEFAULT NULL COMMENT ''工序说明'' AFTER `machine_type`,
     MODIFY COLUMN `machine_type` VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''设备类型'',
     MODIFY COLUMN `process_code` VARCHAR(30) NOT NULL COMMENT ''工序编码''',
    'SELECT ''skip mdm_process alter'' AS note'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 删除旧工艺种子（P00-P09），替换为完整的 CNC 工艺库 45 条
DELETE FROM `mdm_process` WHERE `process_code` LIKE 'P%';

INSERT INTO `mdm_process` (`process_code`, `process_name`, `std_time_min`, `machine_type`, `unit_price`, `comment`) VALUES
-- 下料工序
('PROC-SAW-001', '圆锯下料',   10.0, '圆锯机',       15.00, '棒材/管材切断'),
('PROC-SAW-002', '带锯下料',   15.0, '带锯床',       20.00, '板材/块料切割'),
('PROC-LASER-001','激光切割',  20.0, '激光切割机',   30.00, '薄板精密切割'),
('PROC-WATER-001','水切割',    30.0, '水切割机',     40.00, '厚板/热敏感材料切割'),
-- 车削工序
('PROC-LATHE-001','粗车外圆',  15.0, '数控车床',     25.00, '外圆粗加工'),
('PROC-LATHE-002','精车外圆',  20.0, '数控车床',     35.00, '外圆精加工'),
('PROC-LATHE-003','车内孔',    18.0, '数控车床',     30.00, '内孔加工'),
('PROC-LATHE-004','车端面',    10.0, '数控车床',     20.00, '端面加工'),
('PROC-LATHE-005','车螺纹',    25.0, '数控车床',     40.00, '螺纹车削'),
('PROC-LATHE-006','车槽/切断', 12.0, '数控车床',     22.00, '切槽/切断'),
-- 铣削工序
('PROC-MILL-001', '平面铣削',   20.0, 'CNC铣床',      35.00, '平面加工'),
('PROC-MILL-002', '轮廓铣削',   30.0, 'CNC铣床',      45.00, '外形轮廓加工'),
('PROC-MILL-003', '型腔铣削',   40.0, 'CNC铣床',      55.00, '型腔/凹槽加工'),
('PROC-MILL-004', '钻孔',       8.0, '钻床',          15.00, '钻孔加工'),
('PROC-MILL-005', '攻丝',       15.0, 'CNC铣床',      25.00, '螺纹加工'),
('PROC-MILL-006', '铰孔',       12.0, 'CNC铣床',      20.00, '精密孔加工'),
('PROC-MILL-007', '镗孔',       25.0, 'CNC镗床',      40.00, '大直径精密孔'),
-- 磨削工序
('PROC-GRIND-001','外圆磨',     30.0, '外圆磨床',     50.00, '外圆精磨'),
('PROC-GRIND-002','内圆磨',     35.0, '内圆磨床',     55.00, '内孔精磨'),
('PROC-GRIND-003','平面磨',     25.0, '平面磨床',     45.00, '平面精磨'),
('PROC-GRIND-004','无心磨',     20.0, '无心磨床',     40.00, '棒材外圆精磨'),
('PROC-GRIND-005','工具磨',     25.0, '工具磨床',     45.00, '刀具修磨'),
-- 放电/线切割
('PROC-EDM-001',  '电火花粗加工',60.0,'电火花成型机', 80.00, '放电粗加工'),
('PROC-EDM-002',  '电火花精加工',80.0,'电火花成型机',100.00, '放电精加工'),
('PROC-WEDM-001', '线切割粗加工',50.0,'线切割机',    70.00, '线割粗加工'),
('PROC-WEDM-002', '线切割精加工',70.0,'线切割机',    90.00, '线割精加工'),
-- 热处理工序
('PROC-HEAT-001', '淬火',      120.0, '淬火炉',       60.00, '淬火处理'),
('PROC-HEAT-002', '回火',       90.0, '回火炉',       50.00, '回火处理'),
('PROC-HEAT-003', '调质',      150.0, '热处理炉',     80.00, '淬火+高温回火'),
('PROC-HEAT-004', '渗碳',      240.0, '渗碳炉',      100.00, '表面渗碳'),
('PROC-HEAT-005', '固溶',      180.0, '固溶炉',       90.00, '固溶处理'),
('PROC-HEAT-006', '时效',      120.0, '时效炉',       70.00, '时效处理'),
-- 表面处理工序
('PROC-SURF-001', '阳极氧化（本色）',60.0,'阳极氧化线',25.00,'本色阳极氧化'),
('PROC-SURF-002', '阳极氧化（黑色）',60.0,'阳极氧化线',30.00,'黑色阳极氧化'),
('PROC-SURF-003', '硬质阳极氧化',   90.0,'阳极氧化线',45.00,'硬质阳极氧化'),
('PROC-SURF-004', '电镀锌',           45.0,'电镀线',   20.00,'镀锌'),
('PROC-SURF-005', '电镀铬',           60.0,'电镀线',   35.00,'镀铬'),
('PROC-SURF-006', '电镀镍',           60.0,'电镀线',   40.00,'镀镍'),
('PROC-SURF-007', '喷涂',             30.0,'喷涂线',   25.00,'喷漆/喷粉'),
('PROC-SURF-008', '发黑',             30.0,'发黑线',   15.00,'发黑处理'),
('PROC-SURF-009', '抛光',             20.0,'抛光机',   20.00,'表面抛光'),
('PROC-SURF-010', '喷砂',             15.0,'喷砂机',   18.00,'喷砂处理'),
-- 检测工序
('PROC-CMM-001',  '三次元检测',  45.0, '三次元测量机',60.00, 'CMM 尺寸检测'),
('PROC-CMM-002',  '二次元检测',  20.0, '二次元测量仪',30.00, '2D 尺寸检测'),
('PROC-HARD-001', '硬度检测',    10.0, '硬度计',      15.00, '硬度检测'),
('PROC-ROUGH-001','粗糙度检测',   8.0, '粗糙度仪',    12.00, '表面粗糙度检测'),
-- 辅助工序
('PROC-CLEAN-001','清洗',        15.0, '清洗机',      10.00, '零部件清洗'),
('PROC-DEBURR-001','去毛刺',     20.0, '手工',        15.00, '手工去毛刺'),
('PROC-MARK-001', '打标',        10.0, '打标机',       8.00, '激光打标'),
('PROC-PACK-001', '包装',        10.0, '手工',         5.00, '产品包装')
ON DUPLICATE KEY UPDATE `unit_price` = VALUES(`unit_price`);

-- 创建工序成本映射表
CREATE TABLE IF NOT EXISTS `crm_process_cost_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `keyword` VARCHAR(64) NOT NULL COMMENT '工序名称关键词',
    `cost_item_code` VARCHAR(32) NOT NULL COMMENT '匹配的成本项编码',
    `sort_order` INT DEFAULT 0 COMMENT '匹配优先级',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_keyword` (`keyword`),
    KEY `idx_cost_item_code` (`cost_item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序-成本项自动匹配规则';

-- 种子数据
INSERT IGNORE INTO `crm_process_cost_mapping` (`keyword`, `cost_item_code`, `sort_order`) VALUES
('车',      'LATHE',       1),
('车床',    'LATHE',       1),
('CNC',     'CNC',          2),
('铣',      'CNC',          2),
('钻',      'CNC',          2),
('镗',      'CNC',          2),
('攻丝',    'CNC',          2),
('磨',      'GRIND',       3),
('磨床',    'GRIND',       3),
('电火花',  'EDM',         4),
('放电',    'EDM',         4),
('线切割',  'WEDM',       5),
('WEDM',   'WEDM',       5),
('阳极',   'ANODIZE',     6),
('氧化',   'ANODIZE',     6),
('固溶',   'SOLID_SOLUTION', 7),
('整形',   'FORMING',      8),
('淬火',   'HEAT',        9),
('回火',   'HEAT',        9),
('调质',   'HEAT',        9),
('渗碳',   'HEAT',        9),
('时效',   'HEAT',        9),
('热处理', 'HEAT',        9),
('喷砂',   'SURFACE',     10),
('抛光',   'SURFACE',     10),
('检测',   'INSPECT',     11),
('测量',   'INSPECT',     11),
('三次元', 'INSPECT',     11),
('硬度',   'INSPECT',     11),
('清洗',   'CLEAN',       12),
('包装',   'PACK',        13),
('下料',   'CUTTING',    14),
('锯',     'CUTTING',    14);

-- 补充成本项（幂等）
INSERT IGNORE INTO `crm_quote_cost_item` (`item_code`, `item_name`, `billing_method`, `unit`, `unit_price`, `profit_margin`, `process_code`, `sort_order`) VALUES
('GRIND',   '磨床',   'BY_HOUR',   'h',   80.0000, 0.2000, 'GRIND',   20),
('HEAT',    '热处理', 'BY_HOUR',   'h',  100.0000, 0.2000, 'HEAT',   21),
('SURFACE', '表面处理','BY_AREA',   '㎡',  30.0000, 0.2500, NULL,      22),
('INSPECT', '检测',   'BY_HOUR',   'h',   60.0000, 0.1500, NULL,      23),
('CLEAN',   '清洗',   'BY_HOUR',   'h',   20.0000, 0.1000, NULL,      24),
('CUTTING', '下料',   'BY_HOUR',   'h',   15.0000, 0.1500, NULL,      25),
('PACK',    '包装',   'BY_PIECE',  '件',   5.0000,  0.1000, NULL,      26);

-- ================================================================
-- crm_process 工艺主表 + crm_process_step 工序明细 种子数据
-- erp-production /processes 接口查的是 crm_process
-- ================================================================

INSERT IGNORE INTO `crm_process` (id, process_code, process_name, process_type, description, total_steps, total_estimated_hours, total_cost, is_reusable, is_active, owner_user_id)
VALUES
(1, 'PROC-LATHE-001', '数控车削件工艺', 'STANDARD', '典型数控车削工艺，含粗车、精车、钻孔、攻丝', 4, 0.67, 0, 1, 1, 1),
(2, 'PROC-MILL-001',  'CNC铣削件工艺',  'STANDARD', '典型CNC铣削工艺，含平面铣、轮廓铣、钻孔、攻丝', 4, 0.75, 0, 1, 1, 1),
(3, 'PROC-SURF-001',  '阳极氧化工艺',   'STANDARD', '铝合金阳极氧化表面处理，含本色氧化、喷砂', 2, 1.00, 0, 1, 1, 1),
(4, 'PROC-HEAT-001',  '热处理调质工艺', 'STANDARD', '钢铁调质处理，含淬火、回火', 2, 4.00, 0, 1, 1, 1),
(5, 'PROC-GRIND-001', '磨削精加工工艺', 'STANDARD', '内外圆磨削精加工', 2, 1.00, 0, 1, 1, 1)
ON DUPLICATE KEY UPDATE process_name = VALUES(process_name);

INSERT IGNORE INTO `crm_process_step` (process_id, step_no, step_name, segment, machine_type, estimated_hours, unit_cost, is_quality_check)
VALUES
-- PROC-LATHE-001：车削件工艺（4道工序）
(1, 1, '粗车外圆',         '粗加工', '数控车床',  0.50, 25.0000, 0),
(1, 2, '精车外圆',         '精加工', '数控车床',  0.33, 35.0000, 0),
(1, 3, '钻孔',             '粗加工', '钻床',      0.13, 15.0000, 0),
(1, 4, '攻丝',             '精加工', 'CNC铣床',   0.25, 25.0000, 0),
-- PROC-MILL-001：铣削件工艺（4道工序）
(2, 1, '平面铣削',         '粗加工', 'CNC铣床',   0.33, 35.0000, 0),
(2, 2, '轮廓铣削',         '精加工', 'CNC铣床',   0.50, 45.0000, 0),
(2, 3, '钻孔',             '粗加工', '钻床',      0.13, 15.0000, 0),
(2, 4, '攻丝',             '精加工', 'CNC铣床',   0.25, 25.0000, 0),
-- PROC-SURF-001：阳极氧化工艺（2道工序）
(3, 1, '阳极氧化（本色）', '表面处理', '阳极氧化线', 1.00, 25.0000, 0),
(3, 2, '喷砂',             '表面处理', '喷砂机',     0.50, 18.0000, 0),
-- PROC-HEAT-001：热处理调质工艺（2道工序）
(4, 1, '淬火',             '粗加工', '淬火炉',    2.00, 60.0000, 0),
(4, 2, '回火',             '精加工', '回火炉',    2.00, 50.0000, 0),
-- PROC-GRIND-001：磨削精加工工艺（2道工序）
(5, 1, '外圆磨',           '精加工', '外圆磨床',  0.50, 50.0000, 0),
(5, 2, '平面磨',           '精加工', '平面磨床',  0.50, 45.0000, 0)
ON DUPLICATE KEY UPDATE step_name = VALUES(step_name);

-- ================================================================
-- MACHINE_TYPE 数据字典（统一维护设备类型，供各模块引用）
-- 注意：erp-production 无 dict Feign 客户端，listTypes() 继续读 prod_machine，
--       此处同时补充 prod_machine 种子（至少每类型有 1 台，下拉才有数据）
-- ================================================================

USE `cnc_platform`;

-- 1) 字典类型
INSERT IGNORE INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`)
VALUES ('MACHINE_TYPE', '设备类型', 'CNC 加工厂全设备类型', 1);

-- 2) 设备类型字典项（18 项，覆盖全工序）
INSERT IGNORE INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('MACHINE_TYPE', 'CNC_MILL',   'CNC铣床',          1,  'ACTIVE'),
('MACHINE_TYPE', 'CNC_LATHE',  '数控车床',          2,  'ACTIVE'),
('MACHINE_TYPE', 'LATHE',      '车床',               3,  'ACTIVE'),
('MACHINE_TYPE', 'MILL',       '铣床',               4,  'ACTIVE'),
('MACHINE_TYPE', 'GRINDER',    '磨床',               5,  'ACTIVE'),
('MACHINE_TYPE', 'EDM',        '电火花成型机',        6,  'ACTIVE'),
('MACHINE_TYPE', 'WEDM',       '线切割机',           7,  'ACTIVE'),
('MACHINE_TYPE', 'LASER',      '激光切割机',          8,  'ACTIVE'),
('MACHINE_TYPE', 'WATER',      '水切割机',            9,  'ACTIVE'),
('MACHINE_TYPE', 'SAW',        '圆锯机',              10, 'ACTIVE'),
('MACHINE_TYPE', 'BANDSAW',    '带锯床',              11, 'ACTIVE'),
('MACHINE_TYPE', 'BORE',       'CNC镗床',             12, 'ACTIVE'),
('MACHINE_TYPE', 'DRILL',      '钻床',                13, 'ACTIVE'),
('MACHINE_TYPE', 'HEAT_FURNACE','热处理炉',           14, 'ACTIVE'),
('MACHINE_TYPE', 'ANODIZE',    '阳极氧化线',           15, 'ACTIVE'),
('MACHINE_TYPE', 'PLATE',      '电镀线',               16, 'ACTIVE'),
('MACHINE_TYPE', 'SPRAY',      '喷涂线',               17, 'ACTIVE'),
('MACHINE_TYPE', 'CMM',       '三次元测量机',         18, 'ACTIVE'),
('MACHINE_TYPE', 'MANUAL',     '手工',                 19, 'ACTIVE')
ON DUPLICATE KEY UPDATE `dict_label` = VALUES(`dict_label`);

USE `cnc_business`;

-- 3) prod_machine 种子（每类型至少 1 台，确保 /machines/types 下拉有数据）
INSERT IGNORE INTO `prod_machine` (`id`, `machine_code`, `machine_name`, `machine_type`, `machine_no`, `status`, `last_maintenance`, `maintenance_cycle_days`)
VALUES
(1,  'SB-SAW-001',   '圆锯机',        '圆锯机',       'SAW-01',   'IDLE',     '2026-06-01 08:00:00', 60),
(2,  'SB-BAND-001',  '带锯床',        '带锯床',       'BAND-01',  'IDLE',     '2026-06-02 08:00:00', 60),
(3,  'SB-LASER-001', '激光切割机',    '激光切割机',    'LASER-01', 'IDLE',     '2026-06-03 08:00:00', 90),
(4,  'SB-WATER-001', '水切割机',      '水切割机',     'WATER-01', 'IDLE',     '2026-06-04 08:00:00', 90),
(5,  'SB-MILL-001',  'CNC铣床 01',   'CNC铣床',      'MILL-01',  'IDLE',     '2026-06-05 08:00:00', 90),
(6,  'SB-LATHE-001', '数控车床 01',  '数控车床',     'LATHE-01', 'IDLE',     '2026-06-06 08:00:00', 90),
(7,  'SB-GRIND-001', '外圆磨床',      '磨床',         'GRIND-01', 'IDLE',     '2026-06-07 08:00:00', 120),
(8,  'SB-EDM-001',   '电火花成型机',  '电火花成型机', 'EDM-01',   'IDLE',     '2026-06-08 08:00:00', 90),
(9,  'SB-WEDM-001',  '线切割机',      '线切割机',     'WEDM-01', 'IDLE',     '2026-06-09 08:00:00', 90),
(10, 'SB-HEAT-001',  '热处理炉',      '热处理炉',     'HEAT-01',  'IDLE',     '2026-06-10 08:00:00', 120),
(11, 'SB-ANOD-001',  '阳极氧化线',    '阳极氧化线',   'ANOD-01',  'IDLE',     '2026-06-11 08:00:00', 90),
(12, 'SB-PLATE-001', '电镀线',        '电镀线',       'PLATE-01', 'IDLE',     '2026-06-12 08:00:00', 90),
(13, 'SB-SPRAY-001', '喷涂线',        '喷涂线',       'SPRAY-01', 'IDLE',     '2026-06-13 08:00:00', 90),
(14, 'SB-CMM-001',   '三次元测量机',  '三次元测量机', 'CMM-01',   'IDLE',     '2026-06-14 08:00:00', 90),
(15, 'SB-DRILL-001', '钻床',          '钻床',         'DRILL-01', 'IDLE',     '2026-06-15 08:00:00', 90),
(16, 'SB-BORE-001',  'CNC镗床',      'CNC镗床',      'BORE-01',  'IDLE',     '2026-06-16 08:00:00', 90)
ON DUPLICATE KEY UPDATE `machine_name` = VALUES(`machine_name`);

-- 4) prod_machine_load 种子（对应 16 台机器的负荷数据）
-- 表结构：(machine_id, load_date, planned_hours, available_hours)
INSERT IGNORE INTO `prod_machine_load` (`machine_id`, `load_date`, `planned_hours`, `available_hours`)
VALUES
(1, CURDATE(), 0.0, 12.0),
(2, CURDATE(), 0.0, 12.0),
(3, CURDATE(), 0.0, 12.0),
(4, CURDATE(), 0.0, 12.0),
(5, CURDATE(), 0.0, 12.0),
(6, CURDATE(), 0.0, 12.0),
(7, CURDATE(), 0.0, 12.0),
(8, CURDATE(), 0.0, 12.0),
(9, CURDATE(), 0.0, 12.0),
(10, CURDATE(), 0.0, 12.0),
(11, CURDATE(), 0.0, 12.0),
(12, CURDATE(), 0.0, 12.0),
(13, CURDATE(), 0.0, 12.0),
(14, CURDATE(), 0.0, 12.0),
(15, CURDATE(), 0.0, 12.0),
(16, CURDATE(), 0.0, 12.0)
ON DUPLICATE KEY UPDATE `planned_hours` = VALUES(`planned_hours`);
