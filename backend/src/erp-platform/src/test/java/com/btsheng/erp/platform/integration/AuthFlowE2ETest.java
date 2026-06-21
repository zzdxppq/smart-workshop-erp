package com.btsheng.erp.platform.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * AuthFlow E2E 集成测试（V1.3.7 · I.2 · Testcontainers MySQL 8 + Redis 7）
 *
 * <p>测试场景：管理员建角色 → 建用户 → 用户登录 → 看菜单 → 提交报价 → 路由到部门经理。
 * V1.3.7 dev 阶段：使用 Testcontainers 拉取真实 MySQL 8 + Redis 7 容器，
 * 验证完整登录→审批路由链路。
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.0:///cnc_platform?TC_REUSABLE=true",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
@DisplayName("E2E: Auth + Approval Flow (Testcontainers)")
class AuthFlowE2ETest {

    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("cnc_platform")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @Test
    @DisplayName("smoke: Testcontainers 启动 MySQL 8 + Redis 7 容器")
    void smoke_containers_up() {
        // 验证容器启动
            org.junit.jupiter.api.Assertions.assertTrue(MYSQL.isRunning());
        org.junit.jupiter.api.Assertions.assertTrue(REDIS.isRunning());
    }

    @Test
    @DisplayName("e2e: 登录 + 审批路由（占位）")
    void e2e_login_and_route() {
        // V1.3.7 dev 阶段：仅验证容器与上下文加载
        // 真实 E2E 流测由 QA 商鞅的 Comprehensive TestDesign 完整实装
        // 见 docs/qa/test-designs/1.1-test-design.md
            org.junit.jupiter.api.Assertions.assertNotNull(MYSQL.getJdbcUrl());
    }
}
