package com.btsheng.erp.production.performance.service;

import com.btsheng.erp.production.performance.entity.CrmEmployeePerformanceDaily;
import com.btsheng.erp.production.performance.mapper.CrmEmployeePerformanceDailyMapper;
import com.btsheng.erp.production.performance.mapper.ProductionPerformanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** V1.4.0 · E11-S6 · 绩效日聚合写入 */
@Service
public class PerformanceDailyAggService {

    private static final BigDecimal OUTPUT_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal QUALITY_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal UTIL_WEIGHT = new BigDecimal("0.30");

    private final CrmEmployeePerformanceDailyMapper dailyMapper;
    private final ProductionPerformanceMapper perfMapper;

    @Autowired
    public PerformanceDailyAggService(CrmEmployeePerformanceDailyMapper dailyMapper,
                                      ProductionPerformanceMapper perfMapper) {
        this.dailyMapper = dailyMapper;
        this.perfMapper = perfMapper;
    }

    /**
     * 聚合指定日期（默认昨日）的操作工 + 机台数据写入日表。
     *
     * @return 写入行数
     */
    @Transactional
    public int aggregateForDate(LocalDate statDate) {
        dailyMapper.deleteByStatDate(statDate);

        List<Map<String, Object>> operators = perfMapper.aggregateByOperator(statDate, statDate, null);
        int maxQty = operators.stream().mapToInt(r -> toInt(r.get("finishedQty"))).max().orElse(1);
        if (maxQty <= 0) maxQty = 1;

        int rows = 0;
        for (Map<String, Object> row : operators) {
            CrmEmployeePerformanceDaily e = new CrmEmployeePerformanceDaily();
            e.setStatDate(statDate);
            e.setOperatorId(toLong(row.get("operatorId")));
            e.setOperatorName(String.valueOf(row.get("operatorName")));
            e.setMachineId(null);
            fillMetrics(e, row, maxQty);
            dailyMapper.insert(e);
            rows++;
        }

        List<Map<String, Object>> machines = perfMapper.aggregateByMachine(statDate, statDate, null);
        int maxMachineQty = machines.stream().mapToInt(r -> toInt(r.get("finishedQty"))).max().orElse(1);
        if (maxMachineQty <= 0) maxMachineQty = 1;

        for (Map<String, Object> row : machines) {
            CrmEmployeePerformanceDaily e = new CrmEmployeePerformanceDaily();
            e.setStatDate(statDate);
            e.setOperatorId(null);
            e.setMachineId(toLong(row.get("machineId")));
            e.setMachineCode(String.valueOf(row.get("machineCode")));
            fillMetrics(e, row, maxMachineQty);
            dailyMapper.insert(e);
            rows++;
        }
        return rows;
    }

    private void fillMetrics(CrmEmployeePerformanceDaily e, Map<String, Object> row, int maxQty) {
        int finished = toInt(row.get("finishedQty"));
        int qualified = toInt(row.get("qualifiedQty"));
        int scrap = toInt(row.get("scrapQty"));
        int actualMin = toInt(row.get("actualMinutes"));
        int stdMin = toInt(row.get("stdMinutes"));
        BigDecimal passRate = toDecimal(row.get("passRate"));
        BigDecimal utilRate = toDecimal(row.get("utilizationRate"));

        e.setFinishedQty(finished);
        e.setQualifiedQty(qualified);
        e.setScrapQty(scrap);
        e.setActualMinutes(actualMin);
        e.setStdMinutes(stdMin);
        e.setPassRate(passRate);
        e.setUtilizationRate(utilRate);

        BigDecimal outputScore = BigDecimal.valueOf(finished)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxQty), 2, RoundingMode.HALF_UP);
        BigDecimal score = outputScore.multiply(OUTPUT_WEIGHT)
                .add(passRate.multiply(BigDecimal.valueOf(100)).multiply(QUALITY_WEIGHT))
                .add(utilRate.multiply(BigDecimal.valueOf(100)).multiply(UTIL_WEIGHT))
                .setScale(1, RoundingMode.HALF_UP);
        e.setScore(score);
        e.setGrade(gradeOf(score));
    }

    private String gradeOf(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 90) return "A";
        if (s >= 80) return "B";
        if (s >= 70) return "C";
        return "D";
    }

    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private Long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }

    private BigDecimal toDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
