package com.btsheng.erp.business.crm.hr.performance.service;

import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.mapper.CrmHrEmployeeMapper;
import com.btsheng.erp.business.crm.hr.employee.service.HrEmployeeResolver;
import com.btsheng.erp.business.crm.hr.performance.dto.PerformanceRequest;
import com.btsheng.erp.business.crm.hr.performance.dto.RecruitmentRequest;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrPerformance;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrRecruitment;
import com.btsheng.erp.business.crm.hr.performance.mapper.CrmHrPerformanceMapper;
import com.btsheng.erp.business.crm.hr.performance.mapper.CrmHrRecruitmentMapper;
import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrPerformanceScheme;
import com.btsheng.erp.business.crm.hr.scheme.mapper.CrmHrPerformanceSchemeMapper;
import com.btsheng.erp.business.crm.hr.scheme.mapper.HrPerformanceAggMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.43 · 绩效与招�?Service
 *
 * 3 方法：addPerformance / getPerformance / addRecruitment
 * 招聘单号 HR{yyyyMM}{seq:4}（按月隔离；避免与对账单 RC 冲突�? * 3 P1 修补：绩�?0-100 / 招聘 4 段状�?/ �?1.41 员工
 */
@Service
public class PerformanceRecruitmentService {

    public static final String STATUS_RECRUITING = "RECRUITING";
    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_ONBOARDED = "ONBOARDED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final CrmHrPerformanceMapper performanceMapper;
    private final CrmHrRecruitmentMapper recruitmentMapper;
    private final CrmHrEmployeeMapper employeeMapper;
    private final CrmHrPerformanceSchemeMapper schemeMapper;
    private final HrPerformanceAggMapper aggMapper;
    private final DocNoGenerator docNoGenerator;
    private final HrEmployeeResolver employeeResolver;

    @Autowired
    public PerformanceRecruitmentService(CrmHrPerformanceMapper performanceMapper,
                                         CrmHrRecruitmentMapper recruitmentMapper,
                                         CrmHrEmployeeMapper employeeMapper,
                                         CrmHrPerformanceSchemeMapper schemeMapper,
                                         HrPerformanceAggMapper aggMapper,
                                         DocNoGenerator docNoGenerator,
                                         HrEmployeeResolver employeeResolver) {
        this.performanceMapper = performanceMapper;
        this.recruitmentMapper = recruitmentMapper;
        this.employeeMapper = employeeMapper;
        this.schemeMapper = schemeMapper;
        this.aggMapper = aggMapper;
        this.docNoGenerator = docNoGenerator;
        this.employeeResolver = employeeResolver;
    }

    /**
     * AC-10.3.1 绩效录入
     * P1 修补 1：分�?0-100 边界
     * P1 修补 3：跨 1.41 员工
     */
    @Transactional
    @AuditLog(module = "hr.performance", action = "performance.add")
    public Result<CrmHrPerformance> addPerformance(PerformanceRequest req, Long operatorUserId) {
        if (req.getEmployeeId() == null) return Result.fail(40001, "EMPLOYEE_ID_REQUIRED");
        if (req.getPeriodYear() == null) return Result.fail(40001, "PERIOD_YEAR_REQUIRED");
        if (req.getPeriodMonth() == null || req.getPeriodMonth() < 1 || req.getPeriodMonth() > 12)
            return Result.fail(40001, "PERIOD_MONTH_INVALID");
        // P1 修补 1
            if (req.getScore() == null || req.getScore().signum() < 0
                || req.getScore().compareTo(new BigDecimal("100")) > 0)
            return Result.fail(40003, "SCORE_OUT_OF_RANGE");

        CrmHrEmployee emp = employeeMapper.selectById(req.getEmployeeId());
        if (emp == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");

        CrmHrPerformance p = new CrmHrPerformance();
        p.setEmployeeId(emp.getId());
        p.setEmployeeNo(emp.getEmployeeNo());
        p.setEmployeeName(emp.getName());
        p.setPeriodYear(req.getPeriodYear());
        p.setPeriodMonth(req.getPeriodMonth());
        p.setScore(req.getScore());
        p.setGrade(scoreToGrade(req.getScore()));
        p.setKpiItems(req.getKpiItems());
        p.setComment(req.getComment());
        p.setCreatedBy(operatorUserId);
        p.setCreatedAt(LocalDateTime.now());
        performanceMapper.insert(p);
        return Result.ok(p);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getPerformance(Long employeeId, Integer year, Integer month,
                                                      int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 20;
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = performanceMapper.selectPerformances(employeeId, year, month, size, offset);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> myPerformance(Long userId, Integer year, Integer month, int page, int size) {
        CrmHrEmployee emp = employeeResolver.resolve(userId);
        if (emp == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");
        return getPerformance(emp.getId(), year, month, page, size);
    }

    @Transactional
    @AuditLog(module = "hr.performance", action = "performance.calculateBatch")
    public Result<Map<String, Object>> calculateBatchPerformance(String period, Long operatorUserId) {
        int[] ym = parsePeriod(period);
        if (ym == null) return Result.fail(40001, "PERIOD_INVALID");

        List<CrmHrEmployee> employees = employeeMapper.selectActiveEmployees();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (CrmHrEmployee emp : employees) {
            CrmHrPerformanceScheme scheme = resolveScheme(emp);
            BigDecimal score = computeScore(emp, scheme, ym[0], ym[1]);
            if (score == null) {
                skipped++;
                continue;
            }

            CrmHrPerformance exist = performanceMapper.selectByEmployeeAndPeriod(emp.getId(), ym[0], ym[1]);
            if (exist != null) {
                exist.setScore(score);
                exist.setGrade(scoreToGrade(score));
                exist.setKpiItems(buildKpiSummary(scheme));
                performanceMapper.updateById(exist);
                updated++;
            } else {
                CrmHrPerformance p = new CrmHrPerformance();
                p.setEmployeeId(emp.getId());
                p.setEmployeeNo(emp.getEmployeeNo());
                p.setEmployeeName(emp.getName());
                p.setPeriodYear(ym[0]);
                p.setPeriodMonth(ym[1]);
                p.setScore(score);
                p.setGrade(scoreToGrade(score));
                p.setKpiItems(buildKpiSummary(scheme));
                p.setCreatedBy(operatorUserId);
                p.setCreatedAt(LocalDateTime.now());
                performanceMapper.insert(p);
                created++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("period", period);
        data.put("created", created);
        data.put("updated", updated);
        data.put("skipped", skipped);
        return Result.ok(data);
    }

    private int[] parsePeriod(String period) {
        if (period == null || period.length() < 7) return null;
        try {
            return new int[]{Integer.parseInt(period.substring(0, 4)), Integer.parseInt(period.substring(5, 7))};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CrmHrPerformanceScheme resolveScheme(CrmHrEmployee emp) {
        if (emp.getPerformanceSchemeId() != null) {
            CrmHrPerformanceScheme s = schemeMapper.selectById(emp.getPerformanceSchemeId());
            if (s != null) return s;
        }
        if (emp.getPosition() != null) {
            CrmHrPerformanceScheme s = schemeMapper.selectByPosition(emp.getPosition());
            if (s != null) return s;
        }
        return schemeMapper.selectDefault();
    }

    private BigDecimal computeScore(CrmHrEmployee emp, CrmHrPerformanceScheme scheme, int year, int month) {
        if (scheme == null) return null;
        BigDecimal outputScore = BigDecimal.valueOf(75);
        BigDecimal qualityScore = BigDecimal.valueOf(80);
        BigDecimal attendanceScore = BigDecimal.valueOf(85);

        if (emp.getUserId() != null) {
            Map<String, Object> agg = aggMapper.monthlyAggByOperator(emp.getUserId(), year, month);
            if (agg != null && !agg.isEmpty()) {
                int finished = toInt(agg.get("finishedQty"));
                if (finished > 0) {
                    outputScore = BigDecimal.valueOf(Math.min(100, finished / 5.0));
                    qualityScore = toDecimal(agg.get("passRate")).multiply(BigDecimal.valueOf(100));
                    attendanceScore = toDecimal(agg.get("utilizationRate")).multiply(BigDecimal.valueOf(100));
                }
            }
        }
        int days = aggMapper.attendanceDays(emp.getId(), year, month);
        if (days > 0) {
            attendanceScore = BigDecimal.valueOf(Math.min(100, days * 100.0 / 22.0));
        }

        BigDecimal ow = scheme.getOutputWeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal qw = scheme.getQualityWeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal aw = scheme.getAttendanceWeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return outputScore.multiply(ow).add(qualityScore.multiply(qw)).add(attendanceScore.multiply(aw))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private String buildKpiSummary(CrmHrPerformanceScheme scheme) {
        if (scheme == null) return "自动核算";
        return String.format("产量%.0f%%+质量%.0f%%+出勤%.0f%%",
                scheme.getOutputWeight(), scheme.getQualityWeight(), scheme.getAttendanceWeight());
    }

    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private BigDecimal toDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    /**
     * AC-10.3.2 招聘录入
     * P1 修补 2�? 段状态（RECRUITING/OFFERED/ONBOARDED/REJECTED�?     */
    @Transactional
    @AuditLog(module = "hr.recruitment", action = "recruitment.add")
    public Result<CrmHrRecruitment> addRecruitment(RecruitmentRequest req, Long operatorUserId) {
        if (req.getCandidateName() == null || req.getCandidateName().trim().isEmpty())
            return Result.fail(40001, "CANDIDATE_NAME_REQUIRED");
        if (req.getPosition() == null || req.getPosition().isEmpty())
            return Result.fail(40001, "POSITION_REQUIRED");

        CrmHrRecruitment r = new CrmHrRecruitment();
        r.setRecruitmentNo(docNoGenerator.nextRecruitmentNo());
        r.setCandidateName(req.getCandidateName().trim());
        r.setPosition(req.getPosition());
        r.setDepartment(req.getDepartment());
        r.setPhone(req.getPhone());
        r.setEmail(req.getEmail());
        r.setHrStatus("PENDING");
        r.setDeptStatus("PENDING");
        r.setHrdStatus("PENDING");
        r.setFinalStatus(STATUS_RECRUITING);
        r.setCreatedBy(operatorUserId);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        recruitmentMapper.insert(r);
        return Result.ok(r);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listRecruitments(String finalStatus, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 200) pageSize = 20;
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = recruitmentMapper.selectRecruitments(finalStatus, pageSize, offset);
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows);
        data.put("list", rows);
        data.put("records", rows);
        data.put("total", rows.size());
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.ok(data);
    }

    @Transactional(readOnly = true)
    public Result<CrmHrRecruitment> getRecruitment(Long id) {
        if (id == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "id 必填");
        }
        CrmHrRecruitment r = recruitmentMapper.selectById(id);
        if (r == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "招聘单不存在");
        }
        return Result.ok(r);
    }

    private String scoreToGrade(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) return "A";
        if (score.compareTo(new BigDecimal("80")) >= 0) return "B";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "C";
        return "D";
    }
}
