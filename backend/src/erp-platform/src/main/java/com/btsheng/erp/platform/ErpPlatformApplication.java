package com.btsheng.erp.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * erp-platform · 基础能力服务 (用户/角色/审批/字典/文件/邮件)
 *
 * Port: 8081
 *
 * <p>V1.3.9 Sprint 12 Story 12.2：{@code @EnableScheduling} 启用 60s 心跳调度
 * <p>V1.3.7 Story 1.3：{@code AuditCleanupTask} 凌晨 3 点归档
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.btsheng.erp.platform.**.mapper")
@SpringBootApplication(scanBasePackages = {
    "com.btsheng.erp.platform",
    "com.btsheng.erp.core"
})
@ComponentScan(
    basePackages = {
        "com.btsheng.erp.platform",
        "com.btsheng.erp.core"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.btsheng.erp.core.config.DekBootstrapConfig.class
    )
)
public class ErpPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpPlatformApplication.class, args);
        System.out.println("[ErpPlatformApplication] started on port 8081");
    }
}
