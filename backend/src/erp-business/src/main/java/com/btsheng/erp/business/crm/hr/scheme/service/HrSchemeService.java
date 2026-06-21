package com.btsheng.erp.business.crm.hr.scheme.service;

import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrPerformanceScheme;
import com.btsheng.erp.business.crm.hr.scheme.entity.CrmHrSalaryPackage;
import com.btsheng.erp.business.crm.hr.scheme.mapper.CrmHrPerformanceSchemeMapper;
import com.btsheng.erp.business.crm.hr.scheme.mapper.CrmHrSalaryPackageMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HrSchemeService {

    private final CrmHrPerformanceSchemeMapper schemeMapper;
    private final CrmHrSalaryPackageMapper packageMapper;

    @Autowired
    public HrSchemeService(CrmHrPerformanceSchemeMapper schemeMapper,
                           CrmHrSalaryPackageMapper packageMapper) {
        this.schemeMapper = schemeMapper;
        this.packageMapper = packageMapper;
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listSchemes() {
        List<CrmHrPerformanceScheme> list = schemeMapper.selectList(null);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        return Result.ok(data);
    }

    @Transactional
    public Result<CrmHrPerformanceScheme> saveScheme(CrmHrPerformanceScheme scheme) {
        if (scheme.getId() == null) {
            schemeMapper.insert(scheme);
        } else {
            schemeMapper.updateById(scheme);
        }
        return Result.ok(scheme);
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listPackages() {
        List<CrmHrSalaryPackage> list = packageMapper.selectList(null);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        return Result.ok(data);
    }

    @Transactional
    public Result<CrmHrSalaryPackage> savePackage(CrmHrSalaryPackage pkg) {
        if (pkg.getId() == null) {
            packageMapper.insert(pkg);
        } else {
            packageMapper.updateById(pkg);
        }
        return Result.ok(pkg);
    }
}
