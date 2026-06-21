package com.btsheng.erp.platform.sysparam.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.sysparam.entity.SysParam;
import com.btsheng.erp.platform.sysparam.service.SysParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** V1.3.7 Story 1.3 · AC-1.3.2 · 系统参数 CRUD（4 端点） */
@Tag(name = "E1-Param", description = "系统参数")
@RestController
@RequestMapping("/params")
public class SysParamController {

    private final SysParamService paramService;

    @Autowired
    public SysParamController(SysParamService paramService) { this.paramService = paramService; }

    @Operation(summary = "按组查询参数列表")
    @GetMapping
    public Result<List<SysParam>> listByGroup(@RequestParam(value = "group", required = false) String group) {
        return paramService.listByGroup(group);
    }

    @Operation(summary = "查询单个参数")
    @GetMapping("/{key}")
    public Result<SysParam> getByKey(@PathVariable("key") String key) {
        return paramService.getByKey(key);
    }

    @Operation(summary = "修改参数 + Nacos 推送")
    @PutMapping("/{key}")
    public Result<SysParam> updateParam(@PathVariable("key") String key, @RequestBody Map<String, String> body) {
        return paramService.updateParam(key, body.get("value"), 1L);
    }

    @Operation(summary = "手动触发 Nacos 推送")
    @PostMapping("/refresh")
    public Result<Void> refreshAll() {
        return paramService.refreshAll();
    }
}
