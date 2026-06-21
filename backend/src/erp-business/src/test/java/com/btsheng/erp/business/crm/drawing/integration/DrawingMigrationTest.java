package com.btsheng.erp.business.crm.drawing.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 数据迁移测试（3 测例 · QA 测例组 3）
 *
 * <ul>
 *   <li>TC-12.1.3.1 V54 + data 迁移 SQL 文件存在 · 5 表 JOIN 回填</li>
 *   <li>TC-12.1.3.2 uk_biz_ref UNIQUE 约束存在</li>
 *   <li>TC-12.1.3.3 备份表 crm_drawing_link_backup 存在 · 可回滚</li>
 * </ul>
 *
 * <p>注：SQL 文件内容静态校验（避免需要真实 DB 实例）
 */
@DisplayName("V54 Flyway 迁移 SQL + 备份表存在性校验")
class DrawingMigrationTest {

    @Test
    @DisplayName("TC-12.1.3.1 V54__crm_drawing_link.sql + data migration 文件存在")
    void v54_main_migration_file_exists() throws Exception {
        Path v54 = Paths.get("db/migrations/V54__crm_drawing_link.sql");
        Path data = Paths.get("db/migrations/data/V54__migrate_drawing_link.sql");
        // 文件可能在仓库根目录或 backend 目录下
            assertTrue(Files.exists(v54) || Files.exists(Paths.get("backend/db/migrations/V54__crm_drawing_link.sql")),
                "V54 migration SQL 文件缺失");
        assertTrue(Files.exists(data) || Files.exists(Paths.get("backend/db/migrations/data/V54__migrate_drawing_link.sql")),
                "V54 data migration SQL 文件缺失");

        // 读取内容校验关键字段
            Path v54Actual = Files.exists(v54) ? v54 : Paths.get("backend/db/migrations/V54__crm_drawing_link.sql");
        String content = Files.readString(v54Actual);
        assertTrue(content.contains("crm_drawing_link"), "缺 crm_drawing_link 表");
        assertTrue(content.contains("DRAWING_SCOPE"), "缺 DRAWING_SCOPE 字典");
        assertTrue(content.contains("DRAWING_ACL_FEATURE_FLAG"), "缺 feature flag 字典");
        assertTrue(content.contains("draw.acl.gray.OPERATOR"), "缺 OPERATOR 灰度开关");
        assertTrue(content.contains("crm_drawing_link_backup"), "缺备份表");
        assertTrue(content.contains("ON DELETE RESTRICT"), "缺 RESTRICT 约束");
    }

    @Test
    @DisplayName("TC-12.1.3.2 uk_biz_ref UNIQUE 约束 + 5 类 bizType 注释")
    void uk_biz_ref_constraint() throws Exception {
        Path v54 = resolvePath("V54__crm_drawing_link.sql");
        String content = Files.readString(v54);
        assertTrue(content.contains("uk_biz_ref"), "缺 uk_biz_ref 唯一索引");
        assertTrue(content.contains("UNIQUE KEY uk_biz_ref") || content.contains("UNIQUE KEY `uk_biz_ref`"),
                "uk_biz_ref 非 UNIQUE KEY");

        // data migration 5 类 bizType 全部存在
            Path data = resolveDataPath("V54__migrate_drawing_link.sql");
        String dataContent = Files.readString(data);
        assertTrue(dataContent.contains("'ORDER'"), "缺 ORDER 迁移");
        assertTrue(dataContent.contains("'PO'"), "缺 PO 迁移");
        assertTrue(dataContent.contains("'INCOMING'"), "缺 INCOMING 迁移");
        assertTrue(dataContent.contains("'INSPECTION'"), "缺 INSPECTION 迁移");
        assertTrue(dataContent.contains("'WORKORDER_PROCESS'"), "缺 WORKORDER_PROCESS 迁移");
        assertTrue(dataContent.contains("INSERT IGNORE"), "缺 INSERT IGNORE 防重");
        assertTrue(dataContent.contains("GROUP BY"), "缺 GROUP BY 去重");
    }

    @Test
    @DisplayName("TC-12.1.3.3 crm_drawing_link_backup 备份表 + 回滚 SQL 可执行")
    void backup_table_and_rollback() throws Exception {
        Path v54 = resolvePath("V54__crm_drawing_link.sql");
        String content = Files.readString(v54);
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS crm_drawing_link_backup"),
                "缺 crm_drawing_link_backup 备份表");
        // 验证回滚 SQL 模式存在
            assertTrue(content.contains("INSERT INTO crm_drawing_link_backup SELECT"),
                "缺末尾备份 SQL（V54 末尾备份可回滚）");
    }

    // ============================================================
    // 辅助：跨 cwd 解析路径
    // ============================================================
            private Path resolvePath(String name) throws Exception {
        Path p1 = Paths.get("db/migrations/" + name);
        if (Files.exists(p1)) return p1;
        Path p2 = Paths.get("backend/db/migrations/" + name);
        if (Files.exists(p2)) return p2;
        throw new AssertionError("SQL 文件不存在: " + name);
    }

    private Path resolveDataPath(String name) throws Exception {
        Path p1 = Paths.get("db/migrations/data/" + name);
        if (Files.exists(p1)) return p1;
        Path p2 = Paths.get("backend/db/migrations/data/" + name);
        if (Files.exists(p2)) return p2;
        throw new AssertionError("data SQL 文件不存在: " + name);
    }
}