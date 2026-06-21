package com.btsheng.erp.business.crm.hr.scheme.controller;

import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrPerformanceScheme;
import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrSalaryPackage;
import com.btsheng.erp.business.crm.hr.scheme.service.HrSchemeService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "E10-HR-Scheme", description = "人事·考核方案与工资账套")
@RestController
@RequestMapping("/hr")
public class HrSchemeController {

    private final HrSchemeService schemeService;

    @Autowired
    public HrSchemeController(HrSchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @Operation(summary = "考核方案列表")
    @GetMapping("/performance-schemes")
    public Result<Map<String, Object>> listSchemes() {
        return schemeService.listSchemes();
    }

    @Operation(summary = "保存考核方案")
    @PostMapping("/performance-schemes")
    public Result<CrmHrPerformanceScheme> saveScheme(@RequestBody CrmHrPerformanceScheme scheme) {
        return schemeService.saveScheme(scheme);
    }

    @Operation(summary = "工资账套列表")
    @GetMapping("/salary-packages")
    public Result<Map<String, Object>> listPackages() {
        return schemeService.listPackages();
    }

    @Operation(summary = "保存工资账套")
    @PostMapping("/salary-packages")
    public Result<CrmHrSalaryPackage> savePackage(@RequestBody CrmHrSalaryPackage pkg) {
        return schemeService.savePackage(pkg);
    }
}
