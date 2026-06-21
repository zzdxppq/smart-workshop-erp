package com.btsheng.erp.business.finance.profit.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

/**
 * FR-9-4-2 · 从 sys_param（cnc_platform）读取利润率分级阈值，失败时使用默认值。
 */
@Component
public class ProfitThresholdResolver {

    public static final String KEY_WARNING = "finance.profit.warning-rate";
    public static final String KEY_CRITICAL = "finance.profit.critical-rate";
    public static final String KEY_LOSS = "finance.profit.loss-rate";

    private final JdbcTemplate jdbcTemplate;

    private volatile BigDecimal warningRate = new BigDecimal("10.00");
    private volatile BigDecimal criticalRate = new BigDecimal("5.00");
    private volatile BigDecimal lossRate = BigDecimal.ZERO;

    public ProfitThresholdResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    @Scheduled(fixedRate = 300_000)
    public void refresh() {
        warningRate = loadParam(KEY_WARNING, warningRate);
        criticalRate = loadParam(KEY_CRITICAL, criticalRate);
        lossRate = loadParam(KEY_LOSS, lossRate);
    }

    public BigDecimal getWarningRate() {
        return warningRate;
    }

    public BigDecimal getCriticalRate() {
        return criticalRate;
    }

    public BigDecimal getLossRate() {
        return lossRate;
    }

    /** 按 PRD：&lt;0 深红 · &lt;5% 红 · &lt;10% 黄 */
    public String resolveAlertLevel(BigDecimal profitRate) {
        if (profitRate == null) {
            return ProfitAnalysisService.ALERT_NORMAL;
        }
        if (profitRate.compareTo(lossRate) <= 0) {
            return ProfitAnalysisService.ALERT_CRITICAL;
        }
        if (profitRate.compareTo(criticalRate) < 0) {
            return ProfitAnalysisService.ALERT_CRITICAL;
        }
        if (profitRate.compareTo(warningRate) < 0) {
            return ProfitAnalysisService.ALERT_WARNING;
        }
        return ProfitAnalysisService.ALERT_NORMAL;
    }

    private BigDecimal loadParam(String key, BigDecimal fallback) {
        try {
            List<String> values = jdbcTemplate.query(
                    "SELECT param_value FROM cnc_platform.sys_param WHERE param_key = ? LIMIT 1",
                    (rs, rowNum) -> rs.getString(1),
                    key);
            if (values.isEmpty() || values.get(0) == null || values.get(0).isBlank()) {
                return fallback;
            }
            return new BigDecimal(values.get(0).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
