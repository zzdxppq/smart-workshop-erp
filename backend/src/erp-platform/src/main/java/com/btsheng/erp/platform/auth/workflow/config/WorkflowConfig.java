package com.btsheng.erp.platform.auth.workflow.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 工作流 Nacos 配置（V1.3.7 · Story 1.2 · T0.4）
 *
 * <p>对应 Nacos 配置 {@code app.workflow.*}，通过 {@code @RefreshScope} 实时生效（无需重启服务）。
 *
 * <p>V1.3.7 关键项：
 * <ul>
 *   <li>{@code timeoutHours} - 超时阈值（默认 24h，可改 12/48）</li>
 *   <li>{@code overdueScanCron} - 扫描频率（默认 0 *&#47;30 * * * *）</li>
 *   <li>{@code orSignRequired} - 默认 OR 会签（默认 false，向后兼容 Story 1.1）</li>
 *   <li>{@code notifyChannels} - 4 通道列表（REDIS_STREAM/EMAIL/APP_PUSH/WECHAT_WORK）</li>
 *   <li>{@code retryTimes} / {@code retryBackoffMs} - 失败重试 3 次（指数退避 1s/2s/4s）</li>
 *   <li>{@code timeoutScanLockKey} - 分布式锁 key（V1.3.7 多实例部署）</li>
 *   <li>{@code serviceTokens} - 内部 Service Token（Nacos 配置，architect P2 反馈 ⑤）</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "app.workflow")
@Schema(description = "工作流 Nacos 配置（@RefreshScope 热更新）")
public class WorkflowConfig {

    /** 超时阈值（小时，默认 24） */
    private int timeoutHours = 24;

    /** 超时扫描 cron（默认 0 *&#47;30 * * * * = 每 30 分钟一次） */
    private String overdueScanCron = "0 */30 * * * *";

    /** 是否默认 OR 会签（V1.3.7 P1 修补，默认 false 兼容 Story 1.1） */
    private boolean orSignRequired = false;

    /** 4 通道推送列表（默认全开） */
    private List<String> notifyChannels = Arrays.asList("REDIS_STREAM", "EMAIL", "APP_PUSH", "WECHAT_WORK");

    /** 失败重试次数（默认 3） */
    private int retryTimes = 3;

    /** 失败重试退避（毫秒），默认 [1000, 2000, 4000] 指数退避 */
    private List<Long> retryBackoffMs = Arrays.asList(1000L, 2000L, 4000L);

    /** 分布式锁 key（XXL-JOB 多实例部署） */
    private String timeoutScanLockKey = "app.workflow.timeout-scan-lock";

    /** 分布式锁 TTL（秒，默认 60s） */
    private int timeoutScanLockTtl = 60;

    /** Service Token 配置（architect P2 反馈 ⑤：Nacos 配置 + 避免硬编码） */
    private ServiceTokens serviceTokens = new ServiceTokens();

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Service Token 配置（内部服务调用）")
    public static class ServiceTokens {
        /** erp-business 调 workflow 时的 Service Token */
        private String erpBusiness = "PLACEHOLDER_ERP_BUSINESS_TOKEN";
        /** erp-production 调 workflow 时的 Service Token */
        private String erpProduction = "PLACEHOLDER_ERP_PRODUCTION_TOKEN";
        /** erp-finance 调 workflow 时的 Service Token */
        private String erpFinance = "PLACEHOLDER_ERP_FINANCE_TOKEN";
    }
}
