package com.btsheng.erp.business.crm.hr.payroll.service;

import com.btsheng.erp.business.crm.hr.attendance.mapper.CrmHrAttendanceMapper;
import com.btsheng.erp.business.crm.hr.employee.entity.CrmHrEmployee;
import com.btsheng.erp.business.crm.hr.employee.mapper.CrmHrEmployeeMapper;
import com.btsheng.erp.business.crm.hr.employee.service.HrEmployeeResolver;
import com.btsheng.erp.business.crm.hr.payroll.dto.PayrollCalculateRequest;
import com.btsheng.erp.business.crm.hr.payroll.entity.CrmHrPayroll;
import com.btsheng.erp.business.crm.hr.payroll.mapper.CrmHrPayrollMapper;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrPerformance;
import com.btsheng.erp.business.crm.hr.performance.mapper.CrmHrPerformanceMapper;
import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrSalaryPackage;
import com.btsheng.erp.business.crm.hr.scheme.mapper.CrmHrSalaryPackageMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.integration.client.ProductionPerformanceClient;
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
import java.util.stream.Collectors;

/**
 * V1.3.7 · Story 1.42 · 薪酬自动核算 Service
 *
 * 4 方法：calculatePayroll / getPayroll / getMyPayrollHistory / approvePayroll
 * 单号 PY{yyyyMM}{seq:4}（按月隔离）
 * 3 P1 修补：月份有效�?/ 加班�?1.5 �?/ 个税扣除 5000 起征
 * 个税简版：应税 = 总收�?- 5000，超额部�?10% 税率
 */
@Service
public class PayrollService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PAID = "PAID";

    public static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");
    public static final BigDecimal TAX_RATE = new BigDecimal("0.10");
    public static final BigDecimal OVERTIME_RATE = new BigDecimal("1.5");

    private static final int EXPECTED_WORK_DAYS = 22;
    private static final BigDecimal LATE_DEDUCT_RATE = new BigDecimal("0.10");

    private final CrmHrPayrollMapper payrollMapper;
    private final CrmHrEmployeeMapper employeeMapper;
    private final CrmHrPerformanceMapper performanceMapper;
    private final CrmHrSalaryPackageMapper packageMapper;
    private final CrmHrAttendanceMapper attendanceMapper;
    private final ProductionPerformanceClient productionClient;
    private final PayrollPdfExportService pdfExportService;
    private final DocNoGenerator docNoGenerator;
    private final HrEmployeeResolver employeeResolver;

    @Autowired
    public PayrollService(CrmHrPayrollMapper payrollMapper,
                          CrmHrEmployeeMapper employeeMapper,
                          CrmHrPerformanceMapper performanceMapper,
                          CrmHrSalaryPackageMapper packageMapper,
                          CrmHrAttendanceMapper attendanceMapper,
                          ProductionPerformanceClient productionClient,
                          PayrollPdfExportService pdfExportService,
                          DocNoGenerator docNoGenerator,
                          HrEmployeeResolver employeeResolver) {
        this.payrollMapper = payrollMapper;
        this.employeeMapper = employeeMapper;
        this.performanceMapper = performanceMapper;
        this.packageMapper = packageMapper;
        this.attendanceMapper = attendanceMapper;
        this.productionClient = productionClient;
        this.pdfExportService = pdfExportService;
        this.docNoGenerator = docNoGenerator;
        this.employeeResolver = employeeResolver;
    }

    /**
     * AC-10.2.1 自动核算
     * P1 修补 1：月份有效�?     * P1 修补 2：加班费 = base_salary/174 * hours * 1.5
     * P1 修补 3：个税扣�?5000 起征
     */
    @Transactional
    @AuditLog(module = "hr.payroll", action = "payroll.calculate")
    public Result<CrmHrPayroll> calculatePayroll(PayrollCalculateRequest req, Long operatorUserId) {
        if (req.getEmployeeId() == null) return Result.fail(40001, "EMPLOYEE_ID_REQUIRED");
        if (req.getPeriodYear() == null || req.getPeriodYear() < 2000) return Result.fail(40001, "PERIOD_YEAR_INVALID");
        // P1 修补 1
            if (req.getPeriodMonth() == null || req.getPeriodMonth() < 1 || req.getPeriodMonth() > 12)
            return Result.fail(40001, "PERIOD_MONTH_INVALID");

        CrmHrEmployee emp = employeeMapper.selectById(req.getEmployeeId());
        if (emp == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");

        CrmHrPayroll exist = payrollMapper.selectByEmployeeAndPeriod(
                req.getEmployeeId(), req.getPeriodYear(), req.getPeriodMonth());
        if (exist != null) return Result.fail(40901, "PAYROLL_ALREADY_EXISTS");

        BigDecimal base = emp.getBaseSalary() == null ? BigDecimal.ZERO : emp.getBaseSalary();
        // P1 修补 2：加班费 1.5 �?
            BigDecimal hourRate = base.divide(new BigDecimal("174"), 4, RoundingMode.HALF_UP);
        BigDecimal overtimePay = hourRate.multiply(req.getOvertimeHours() == null ? BigDecimal.ZERO : req.getOvertimeHours())
                .multiply(OVERTIME_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = req.getBonus() == null ? BigDecimal.ZERO : req.getBonus();
        BigDecimal deduction = req.getDeduction() == null ? BigDecimal.ZERO : req.getDeduction();

        BigDecimal gross = base.add(overtimePay).add(bonus).subtract(deduction);
        // P1 修补 3�?000 起征
            BigDecimal taxBase = gross.subtract(TAX_THRESHOLD);
        if (taxBase.signum() < 0) taxBase = BigDecimal.ZERO;
        BigDecimal tax = taxBase.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(tax).setScale(2, RoundingMode.HALF_UP);

        CrmHrPayroll p = new CrmHrPayroll();
        p.setPayrollNo(docNoGenerator.nextPayrollNo());
        p.setPeriodYear(req.getPeriodYear());
        p.setPeriodMonth(req.getPeriodMonth());
        p.setEmployeeId(emp.getId());
        p.setEmployeeNo(emp.getEmployeeNo());
        p.setEmployeeName(emp.getName());
        p.setBaseSalary(base);
        p.setOvertimeHours(req.getOvertimeHours() == null ? BigDecimal.ZERO : req.getOvertimeHours());
        p.setOvertimePay(overtimePay);
        p.setBonus(bonus);
        p.setDeduction(deduction);
        p.setTax(tax);
        p.setNetSalary(net);
        p.setStatus(STATUS_DRAFT);
        p.setCreatedBy(operatorUserId);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        payrollMapper.insert(p);
        return Result.ok(p);
    }

    @Transactional
    @AuditLog(module = "hr.payroll", action = "payroll.calculateBatch")
    public Result<Map<String, Object>> calculateBatchPayroll(String period, Long operatorUserId) {
        int[] ym = parsePeriod(period);
        if (ym == null) return Result.fail(40001, "PERIOD_INVALID");

        Map<Long, Integer> pieceQtyByUser = loadPieceQty(ym[0], ym[1]);
        List<CrmHrEmployee> employees = employeeMapper.selectActiveEmployees();
        int created = 0;
        int skipped = 0;

        for (CrmHrEmployee emp : employees) {
            CrmHrPayroll exist = payrollMapper.selectByEmployeeAndPeriod(emp.getId(), ym[0], ym[1]);
            if (exist != null) {
                skipped++;
                continue;
            }
            CrmHrPayroll p = buildPayroll(emp, ym[0], ym[1], pieceQtyByUser, operatorUserId);
            payrollMapper.insert(p);
            created++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("period", period);
        data.put("created", created);
        data.put("skipped", skipped);
        return Result.ok(data);
    }

    @Transactional(readOnly = true)
    public Result<String> payrollSlipHtml(Long id) {
        CrmHrPayroll p = payrollMapper.selectById(id);
        if (p == null) return Result.fail(40401, "PAYROLL_NOT_FOUND");
        String html = """
                <html><head><meta charset="utf-8"><title>工资条</title></head><body>
                <h2>工资条 · %s</h2>
                <p>员工：%s（%s） · 期间：%d-%02d</p>
                <table border="1" cellpadding="8" cellspacing="0">
                <tr><td>基本工资</td><td>%s</td></tr>
                <tr><td>岗位工资</td><td>%s</td></tr>
                <tr><td>计件工资</td><td>%s</td></tr>
                <tr><td>绩效奖金</td><td>%s</td></tr>
                <tr><td>加班费</td><td>%s</td></tr>
                <tr><td>全勤奖</td><td>%s</td></tr>
                <tr><td>扣款</td><td>%s</td></tr>
                <tr><td>社保</td><td>%s</td></tr>
                <tr><td>个税</td><td>%s</td></tr>
                <tr><td><b>实发工资</b></td><td><b>%s</b></td></tr>
                </table></body></html>
                """.formatted(
                p.getPayrollNo(), p.getEmployeeName(), p.getEmployeeNo(),
                p.getPeriodYear(), p.getPeriodMonth(),
                p.getBaseSalary(), nz(p.getPositionSalary()), nz(p.getPiecePay()), nz(p.getPerformanceBonus()),
                nz(p.getOvertimePay()), nz(p.getFullAttendanceBonus()), nz(p.getDeduction()),
                nz(p.getSocialInsurance()), nz(p.getTax()), p.getNetSalary());
        return Result.ok(html);
    }

    @Transactional(readOnly = true)
    public Result<byte[]> payrollSlipPdf(Long id) {
        CrmHrPayroll p = payrollMapper.selectById(id);
        return pdfExportService.exportPdf(p);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getMyPayrollForUser(Long userId, Integer year, Integer month,
                                                           String status, int page, int size) {
        CrmHrEmployee emp = employeeResolver.resolve(userId);
        if (emp == null) return Result.fail(40401, "EMPLOYEE_NOT_FOUND");
        return getMyPayrollHistory(emp.getId(), year, month, status, page, size);
    }

    private record AttendanceCalc(BigDecimal deduction, BigDecimal fullAttendanceBonus) {}

    private AttendanceCalc computeAttendance(CrmHrEmployee emp, int year, int month, CrmHrSalaryPackage pkg,
                                             BigDecimal base) {
        int present = attendanceMapper.countPresentDays(emp.getId(), year, month);
        int late = attendanceMapper.countLateDays(emp.getId(), year, month);
        int absent = Math.max(0, EXPECTED_WORK_DAYS - present);
        BigDecimal dailyRate = base.divide(BigDecimal.valueOf(EXPECTED_WORK_DAYS), 4, RoundingMode.HALF_UP);
        BigDecimal deduction = dailyRate.multiply(BigDecimal.valueOf(absent))
                .add(dailyRate.multiply(LATE_DEDUCT_RATE).multiply(BigDecimal.valueOf(late)))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal fullBonus = (present >= EXPECTED_WORK_DAYS && late == 0 && pkg != null)
                ? nz(pkg.getFullAttendanceBonus()) : BigDecimal.ZERO;
        return new AttendanceCalc(deduction, fullBonus);
    }

    private CrmHrPayroll buildPayroll(CrmHrEmployee emp, int year, int month,
                                        Map<Long, Integer> pieceQtyByUser, Long operatorUserId) {
        CrmHrSalaryPackage pkg = resolvePackage(emp);
        BigDecimal base = pkg != null && pkg.getBaseSalary() != null ? pkg.getBaseSalary() : nz(emp.getBaseSalary());
        BigDecimal positionSalary = pkg != null ? nz(pkg.getPositionSalary()) : BigDecimal.ZERO;

        int qualifiedQty = emp.getUserId() != null ? pieceQtyByUser.getOrDefault(emp.getUserId(), 0) : 0;
        BigDecimal pieceUnit = pkg != null ? nz(pkg.getPieceUnitPrice()) : BigDecimal.ZERO;
        BigDecimal piecePay = pieceUnit.multiply(BigDecimal.valueOf(qualifiedQty)).setScale(2, RoundingMode.HALF_UP);

        CrmHrPerformance perf = performanceMapper.selectByEmployeeAndPeriod(emp.getId(), year, month);
        BigDecimal perfBonus = performanceBonus(base, perf, pkg);

        AttendanceCalc att = computeAttendance(emp, year, month, pkg, base);
        BigDecimal fullBonus = att.fullAttendanceBonus();
        BigDecimal overtimePay = BigDecimal.ZERO;
        BigDecimal deduction = att.deduction();

        BigDecimal gross = base.add(positionSalary).add(piecePay).add(perfBonus)
                .add(overtimePay).add(fullBonus).subtract(deduction);
        BigDecimal socialRate = pkg != null ? nz(pkg.getSocialInsuranceRate()) : new BigDecimal("0.105");
        BigDecimal social = base.multiply(socialRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal threshold = pkg != null ? nz(pkg.getTaxThreshold()) : TAX_THRESHOLD;
        BigDecimal taxRate = pkg != null ? nz(pkg.getTaxRate()) : TAX_RATE;
        BigDecimal taxBase = gross.subtract(social).subtract(threshold);
        if (taxBase.signum() < 0) taxBase = BigDecimal.ZERO;
        BigDecimal tax = taxBase.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(social).subtract(tax).setScale(2, RoundingMode.HALF_UP);

        CrmHrPayroll p = new CrmHrPayroll();
        p.setPayrollNo(docNoGenerator.nextPayrollNo());
        p.setPeriodYear(year);
        p.setPeriodMonth(month);
        p.setEmployeeId(emp.getId());
        p.setEmployeeNo(emp.getEmployeeNo());
        p.setEmployeeName(emp.getName());
        p.setBaseSalary(base);
        p.setPositionSalary(positionSalary);
        p.setPiecePay(piecePay);
        p.setPerformanceBonus(perfBonus);
        p.setOvertimeHours(BigDecimal.ZERO);
        p.setOvertimePay(overtimePay);
        p.setBonus(perfBonus);
        p.setFullAttendanceBonus(fullBonus);
        p.setDeduction(deduction);
        p.setSocialInsurance(social);
        p.setTax(tax);
        p.setNetSalary(net);
        p.setStatus(STATUS_DRAFT);
        p.setCreatedBy(operatorUserId);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private BigDecimal performanceBonus(BigDecimal base, CrmHrPerformance perf, CrmHrSalaryPackage pkg) {
        if (perf == null || pkg == null) return BigDecimal.ZERO;
        BigDecimal rate = switch (perf.getGrade() != null ? perf.getGrade() : "C") {
            case "A" -> nz(pkg.getPerformanceARate());
            case "B" -> nz(pkg.getPerformanceBRate());
            case "D" -> nz(pkg.getPerformanceDRate());
            default -> nz(pkg.getPerformanceCRate());
        };
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private CrmHrSalaryPackage resolvePackage(CrmHrEmployee emp) {
        if (emp.getSalaryPackageId() != null) {
            CrmHrSalaryPackage p = packageMapper.selectById(emp.getSalaryPackageId());
            if (p != null) return p;
        }
        if (emp.getPosition() != null) {
            CrmHrSalaryPackage p = packageMapper.selectByPosition(emp.getPosition());
            if (p != null) return p;
        }
        return packageMapper.selectDefault();
    }

    private Map<Long, Integer> loadPieceQty(int year, int month) {
        try {
            Result<List<Map<String, Object>>> r = productionClient.pieceWages(year, month);
            if (r == null || r.getData() == null) return Map.of();
            return r.getData().stream().collect(Collectors.toMap(
                    m -> ((Number) m.get("operatorUserId")).longValue(),
                    m -> ((Number) m.get("qualifiedQty")).intValue(),
                    (a, b) -> a));
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private int[] parsePeriod(String period) {
        if (period == null || period.length() < 7) return null;
        try {
            return new int[]{Integer.parseInt(period.substring(0, 4)), Integer.parseInt(period.substring(5, 7))};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Transactional(readOnly = true)
    public Result<CrmHrPayroll> getPayroll(Long id) {
        if (id == null) return Result.fail(40001, "PAYROLL_ID_REQUIRED");
        CrmHrPayroll p = payrollMapper.selectById(id);
        if (p == null) return Result.fail(40401, "PAYROLL_NOT_FOUND");
        return Result.ok(p);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getMyPayrollHistory(Long employeeId, Integer year, Integer month,
                                                           String status, int page, int size) {
        if (employeeId == null) return Result.fail(40001, "EMPLOYEE_ID_REQUIRED");
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 20;
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = payrollMapper.selectPayrolls(employeeId, year, month, status, size, offset);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    @Transactional
    @AuditLog(module = "hr.payroll", action = "payroll.approve")
    public Result<CrmHrPayroll> approvePayroll(Long id, Long operatorUserId) {
        CrmHrPayroll p = payrollMapper.selectById(id);
        if (p == null) return Result.fail(40401, "PAYROLL_NOT_FOUND");
        if (!STATUS_DRAFT.equals(p.getStatus())) return Result.fail(40901, "PAYROLL_STATUS_INVALID");
        p.setStatus(STATUS_APPROVED);
        p.setApprovedBy(operatorUserId);
        p.setApprovedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        payrollMapper.updateById(p);
        return Result.ok(p);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listPayrolls(String period, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 200) pageSize = 20;
        Integer year = null;
        Integer month = null;
        if (period != null && !period.isBlank() && period.length() >= 7) {
            try {
                year = Integer.parseInt(period.substring(0, 4));
                month = Integer.parseInt(period.substring(5, 7));
            } catch (NumberFormatException ignored) {
                // use null filters
            }
        }
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = payrollMapper.selectPayrolls(null, year, month, null, pageSize, offset);
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows);
        data.put("list", rows);
        data.put("records", rows);
        data.put("total", rows.size());
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.ok(data);
    }
}
