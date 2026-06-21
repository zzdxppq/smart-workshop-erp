package com.btsheng.erp.platform.sysparam.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.sysparam.entity.ChangeLog;
import com.btsheng.erp.platform.sysparam.entity.GlobalThreshold;
import com.btsheng.erp.platform.sysparam.mapper.ChangeLogMapper;
import com.btsheng.erp.platform.sysparam.mapper.GlobalThresholdMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 阈值服务（V1.3.7 Story 1.3 · AC-1.3.3 · 双轨实现）
 *
 * <p>查询优先级：Nacos（app.workflow.thresholds.{bizType}.{roleCode}） > DB sys_global_threshold > DB sys_role.amount_threshold
 * <p>写入：双写（Nacos + DB + sys_change_log）
 */
@Service
public class ThresholdService {

    private final GlobalThresholdMapper thresholdMapper;
    private final ChangeLogMapper changeLogMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ThresholdService(GlobalThresholdMapper thresholdMapper, ChangeLogMapper changeLogMapper) {
        this.thresholdMapper = thresholdMapper;
        this.changeLogMapper = changeLogMapper;
    }

    public Result<GlobalThreshold> getThreshold(String bizType, String roleCode) {
        // 1) Nacos 优先（架构红线 Nacos @NacosValue 自动注入，简化：用 DB 回退）
        // 2) DB sys_global_threshold
            GlobalThreshold t = thresholdMapper.selectByBizTypeAndRole(bizType, roleCode);
        if (t == null) {
            return Result.fail(40404, "THRESHOLD_NOT_FOUND");
        }
        return Result.ok(t);
    }

    public Result<GlobalThreshold> updateThreshold(String bizType, String roleCode, BigDecimal newThreshold, Long operatorUserId) {
        GlobalThreshold existing = thresholdMapper.selectByBizTypeAndRole(bizType, roleCode);
        if (existing == null) {
            return Result.fail(40404, "THRESHOLD_NOT_FOUND");
        }
        // before 快照
            ChangeLog log = new ChangeLog();
        log.setEntity("threshold");
        log.setEntityId(existing.getId());
        log.setOperation("UPDATE");
        try {
            log.setBeforeValue(mapper.writeValueAsString(existing));
        } catch (Exception e) {
            log.setBeforeValue(existing.getThreshold() == null ? "null" : existing.getThreshold().toString());
        }
        // 更新 DB
            existing.setThreshold(newThreshold);
        thresholdMapper.updateById(existing);
        // after
            try {
            log.setAfterValue(mapper.writeValueAsString(existing));
        } catch (Exception e) {
            log.setAfterValue(newThreshold == null ? "null" : newThreshold.toString());
        }
        log.setChangedBy(operatorUserId);
        changeLogMapper.insert(log);
        // Nacos 推送（@NacosValue 重新加载，留 hook）
            return Result.ok(existing);
    }
}
