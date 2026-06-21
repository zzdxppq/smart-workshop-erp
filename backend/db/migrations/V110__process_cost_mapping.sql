-- V110 · 工序-成本项自动匹配规则（V1.3.9 CNC工艺库报价联动）
USE `cnc_business`;

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
