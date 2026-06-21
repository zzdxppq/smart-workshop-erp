-- V71 · 图纸 CAD/CAM 附件表（FR-3-2-2）
USE `cnc_business`;

CREATE TABLE IF NOT EXISTS `crm_drawing_attachment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `drawing_id` BIGINT NOT NULL COMMENT '图纸 ID',
    `file_name` VARCHAR(256) NOT NULL COMMENT '原始文件名',
    `file_type` VARCHAR(16) NOT NULL COMMENT 'DXF/STEP/NC/DWG/PDF',
    `file_path` VARCHAR(512) NOT NULL COMMENT 'MinIO 或本地路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '字节',
    `uploaded_by` BIGINT DEFAULT NULL COMMENT '上传人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_drawing_id` (`drawing_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸 CAD/CAM 附件（Story 1.7 · FR-3-2-2）';
