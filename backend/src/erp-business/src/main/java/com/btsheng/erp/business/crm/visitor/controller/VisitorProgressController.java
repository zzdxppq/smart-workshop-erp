package com.btsheng.erp.business.crm.visitor.controller;

import com.btsheng.erp.business.crm.visitor.service.VisitorProgressService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** V1.4.0 · E11-S7 · 客户现场演示（脱敏进度查询） */
@RestController
@RequestMapping("/visitor/progress")
@Tag(name = "E11-Visitor-Progress", description = "客户现场演示视图")
@PreAuthorize("hasRole('CUSTOMER_VISITOR') or hasAnyRole('ADMIN', 'GM', 'PROD_MGR', 'PRODUCTION_MANAGER')")
public class VisitorProgressController {

    private final VisitorProgressService service;

    @Autowired
    public VisitorProgressController(VisitorProgressService service) {
        this.service = service;
    }

    @GetMapping("/search")
    @Operation(summary = "按料号/工单/客户搜索生产进度（脱敏）")
    public Result<Map<String, Object>> search(@RequestParam("keyword") String keyword) {
        return service.search(keyword);
    }

    @GetMapping("/list")
    @Operation(summary = "默认活跃工单列表（V1.4.0 · 无 keyword）")
    public Result<Map<String, Object>> list(
            @RequestParam(value = "limit", defaultValue = "23") Integer limit) {
        return service.activeList(limit);
    }

    @GetMapping("/detail")
    @Operation(summary = "工单详情 + 工序时间线（V1.4.0 · 脱敏）")
    public Result<Map<String, Object>> detail(@RequestParam("workorderNo") String workorderNo) {
        return service.detail(workorderNo);
    }
}
