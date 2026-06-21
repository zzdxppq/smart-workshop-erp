package com.btsheng.erp.platform.sysparam.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.sysparam.entity.GlobalThreshold;
import com.btsheng.erp.platform.sysparam.service.ThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** V1.3.7 Story 1.3 · AC-1.3.3 · 金额阈值全局配置（1 端点） */
@Tag(name = "E1-Threshold", description = "金额阈值全局")
@RestController
@RequestMapping("/thresholds")
public class ThresholdController {

    private final ThresholdService thresholdService;

    @Autowired
    public ThresholdController(ThresholdService thresholdService) { this.thresholdService = thresholdService; }

    @Operation(summary = "改阈值 + Nacos 推送 + sys_change_log")
    @PutMapping
    public Result<GlobalThreshold> updateThreshold(@RequestBody Map<String, Object> body) {
        String bizType = (String) body.get("bizType");
        String roleCode = (String) body.get("roleCode");
        BigDecimal value = new BigDecimal(String.valueOf(body.get("threshold")));
        return thresholdService.updateThreshold(bizType, roleCode, value, 1L);
    }

    @Operation(summary = "查询阈值")
    @GetMapping
    public Result<GlobalThreshold> getThreshold(@RequestParam("bizType") String bizType,
                                                 @RequestParam("roleCode") String roleCode) {
        return thresholdService.getThreshold(bizType, roleCode);
    }
}
