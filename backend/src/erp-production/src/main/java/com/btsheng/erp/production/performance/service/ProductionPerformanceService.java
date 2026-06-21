package com.btsheng.erp.production.performance.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.performance.mapper.CrmEmployeePerformanceDailyMapper;
import com.btsheng.erp.production.performance.mapper.ProductionPerformanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** V1.4.0 · E11-S6 · 报工聚合绩效 */
@Service
public class ProductionPerformanceService {

    private static final BigDecimal OUTPUT_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal QUALITY_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal UTIL_WEIGHT = new BigDecimal("0.30");

    private final ProductionPerformanceMapper mapper;
    private final CrmEmployeePerformanceDailyMapper dailyMapper;

    @Autowired
    public ProductionPerformanceService(ProductionPerformanceMapper mapper,
                                        CrmEmployeePerformanceDailyMapper dailyMapper) {
        this.mapper = mapper;
        this.dailyMapper = dailyMapper;
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getBoard(String period, String groupBy, Long userId, String roles) {
        LocalDate[] range = periodRange(period);
        boolean operatorOnly = isOperatorOnly(roles);
        Long scopeUserId = operatorOnly ? userId : null;

        String gb = groupBy != null ? groupBy.toLowerCase(Locale.ROOT) : "operator";
        List<Map<String, Object>> rows;
        if ("machine".equals(gb)) {
            rows = mapper.aggregateByMachine(range[0], range[1], scopeUserId);
        } else {
            rows = mapper.aggregateByOperator(range[0], range[1], scopeUserId);
        }
        enrichScores(rows);

        List<Map<String, Object>> operatorRows = "machine".equals(gb)
                ? mapper.aggregateByOperator(range[0], range[1], scopeUserId)
                : rows;
        if (!"machine".equals(gb)) {
            operatorRows = rows;
        } else {
            enrichScores(operatorRows);
        }
        List<Map<String, Object>> machineRows = "operator".equals(gb)
                ? mapper.aggregateByMachine(range[0], range[1], scopeUserId)
                : rows;
        if (!"operator".equals(gb)) {
            machineRows = rows;
        } else {
            enrichScores(machineRows);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("period", period);
        data.put("groupBy", gb);
        data.put("from", range[0]);
        data.put("to", range[1]);
        data.put("list", rows);
        data.put("operatorList", operatorRows);
        data.put("machineList", machineRows);
        data.put("summary", buildSummary(operatorRows, machineRows));
        data.put("total", rows.size());
        return Result.ok(data);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getOperatorDetail(Long operatorId, String period,
                                                          Long userId, String roles) {
        if (operatorId == null) return Result.fail(40001, "OPERATOR_ID_REQUIRED");
        if (isOperatorOnly(roles) && userId != null && !userId.equals(operatorId)) {
            return Result.fail(40301, "FORBIDDEN");
        }
        LocalDate[] range = periodRange(period);
        Map<String, Object> data = new HashMap<>();
        data.put("operatorId", operatorId);
        data.put("period", period);
        data.put("from", range[0]);
        data.put("to", range[1]);
        data.put("daily", mapper.operatorDailyDetail(range[0], range[1], operatorId));
        data.put("processes", mapper.operatorProcessDistribution(range[0], range[1], operatorId));
        data.put("defects", mapper.operatorDefectRecords(range[0], range[1], operatorId));
        return Result.ok(data);
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> operatorRows,
                                             List<Map<String, Object>> machineRows) {
        Map<String, Object> summary = new HashMap<>();
        int totalQty = operatorRows.stream().mapToInt(r -> toInt(r.get("finishedQty"))).sum();
        double avgPass = operatorRows.isEmpty() ? 0
                : operatorRows.stream().mapToDouble(r -> toDecimal(r.get("passRate")).doubleValue()).average().orElse(0);
        int onDuty = (int) operatorRows.stream().filter(r -> toInt(r.get("finishedQty")) > 0).count();
        int totalStaff = Math.max(operatorRows.size(), onDuty);
        double avgUtil = machineRows.isEmpty() ? 0
                : machineRows.stream().mapToDouble(r -> toDecimal(r.get("utilizationRate")).doubleValue()).average().orElse(0);
        summary.put("totalQty", totalQty);
        summary.put("avgPassRate", BigDecimal.valueOf(avgPass).setScale(4, RoundingMode.HALF_UP));
        summary.put("onDutyCount", onDuty);
        summary.put("totalStaff", totalStaff);
        summary.put("equipmentUtilization", BigDecimal.valueOf(avgUtil).setScale(4, RoundingMode.HALF_UP));
        return summary;
    }

    @Transactional(readOnly = true)
    public Result<List<Map<String, Object>>> getTrend(int days, Long userId, String roles) {
        int d = Math.min(Math.max(days, 7), 90);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(d - 1L);
        Long scopeUserId = isOperatorOnly(roles) ? userId : null;
        List<Map<String, Object>> trend = dailyMapper.trendFromAggTable(start, end, scopeUserId);
        if (trend == null || trend.isEmpty()) {
            trend = mapper.dailyTrend(start, end, scopeUserId);
        }
        return Result.ok(trend);
    }

    @Transactional(readOnly = true)
    public Result<List<Map<String, Object>>> getPieceWages(int year, int month) {
        if (year < 2000 || month < 1 || month > 12) {
            return Result.fail(40001, "PERIOD_INVALID");
        }
        return Result.ok(mapper.pieceWageByOperator(year, month));
    }

    private void enrichScores(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return;
        int maxQty = rows.stream()
                .mapToInt(r -> toInt(r.get("finishedQty")))
                .max().orElse(1);
        if (maxQty <= 0) maxQty = 1;

        for (Map<String, Object> row : rows) {
            int finished = toInt(row.get("finishedQty"));
            BigDecimal passRate = toDecimal(row.get("passRate"));
            BigDecimal utilRate = toDecimal(row.get("utilizationRate"));

            BigDecimal outputScore = BigDecimal.valueOf(finished)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(maxQty), 2, RoundingMode.HALF_UP);
            BigDecimal qualityScore = passRate.multiply(BigDecimal.valueOf(100));
            BigDecimal utilScore = utilRate.multiply(BigDecimal.valueOf(100));

            BigDecimal score = outputScore.multiply(OUTPUT_WEIGHT)
                    .add(qualityScore.multiply(QUALITY_WEIGHT))
                    .add(utilScore.multiply(UTIL_WEIGHT))
                    .setScale(1, RoundingMode.HALF_UP);
            row.put("score", score);
            row.put("grade", gradeOf(score));
            row.put("overRate", finished > 0 && maxQty > 0
                    ? BigDecimal.valueOf(finished).divide(BigDecimal.valueOf(maxQty), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            row.put("achievementRate", row.get("overRate"));
            if (row.containsKey("faultFlag")) {
                int fault = toInt(row.get("faultFlag"));
                row.put("faultRate", fault > 0 ? new BigDecimal("0.15") : BigDecimal.ZERO);
            }
        }
    }

    private String gradeOf(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 90) return "A";
        if (s >= 80) return "B";
        if (s >= 70) return "C";
        return "D";
    }

    private boolean isOperatorOnly(String roles) {
        if (roles == null || roles.isBlank()) return false;
        String r = roles.toUpperCase(Locale.ROOT);
        if (r.contains("PROD_MGR") || r.contains("PRODUCTION_MANAGER") || r.contains("GM") || r.contains("ADMIN")) {
            return false;
        }
        return r.contains("OPERATOR");
    }

    private LocalDate[] periodRange(String period) {
        LocalDate end = LocalDate.now();
        LocalDate start = switch (period != null ? period.toLowerCase(Locale.ROOT) : "day") {
            case "week" -> end.minusDays(6);
            case "month" -> end.minusDays(29);
            default -> end;
        };
        return new LocalDate[]{start, end};
    }

    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private BigDecimal toDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
