package com.btsheng.erp.platform.printer.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import com.btsheng.erp.platform.printer.entity.SysPrinter;
import com.btsheng.erp.platform.printer.service.SysPrinterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 打印机管理 Controller（V1.3.9 Sprint 12 · Story 12.2 · AC-12.2.1/12.2.2/12.2.3）
 *
 * <p>6 端点：4 CRUD（admin） + 1 /test（admin） + 1 /available（任意登录）
 * <p>权限：{@code hasAnyRole('ADMIN','SYS_ADMIN')} 与 sys_dict/audit 一致
 * <p>12.4 引入 sys_print_log 后，DELETE 端点切换到 deletePrinterWithRefCheck
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Tag(name = "E12-Printer", description = "打印机管理 · admin CRUD + 心跳测试 + 可用查询")
@RestController
@RequestMapping("/printers")
public class SysPrinterController {

    private final SysPrinterService printerService;

    @Autowired
    public SysPrinterController(SysPrinterService printerService) {
        this.printerService = printerService;
    }

    /**
     * TC-12.2.1.1 - 列表查询（分页 + 多维过滤）
     */
    @Operation(summary = "查打印机列表（分页 + 多维过滤）")
    @GetMapping
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<PageResponse<SysPrinter>> listPrinters(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return printerService.listPrinters(type, status, enabled, pageNum, pageSize, tenantId);
    }

    /**
     * TC-12.2.1.1/1.2/1.3 - 创建打印机
     */
    @Operation(summary = "新增打印机配置")
    @PostMapping
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<SysPrinter> createPrinter(@RequestBody SysPrinter printer,
                                            @RequestParam(defaultValue = "1") Long operatorUserId,
                                            @RequestParam(defaultValue = "1") Long tenantId) {
        return printerService.createPrinter(printer, operatorUserId, tenantId);
    }

    /**
     * TC-12.2.1.4 - 更新打印机
     */
    @Operation(summary = "修改打印机配置")
    @PutMapping("/{id}")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<SysPrinter> updatePrinter(@PathVariable("id") Long id,
                                            @RequestBody SysPrinter printer,
                                            @RequestParam(defaultValue = "1") Long operatorUserId) {
        return printerService.updatePrinter(id, printer, operatorUserId);
    }

    /**
     * TC-12.2.1.5/1.6 - 删除打印机
     */
    @Operation(summary = "删除打印机配置")
    @DeleteMapping("/{id}")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<Void> deletePrinter(@PathVariable("id") Long id) {
        return printerService.deletePrinter(id);
    }

    /**
     * TC-12.2.2.x - 测试打印机连接（2s TCP 探活）
     */
    @Operation(summary = "测试打印机连接（TCP Socket 探活 · 2 秒超时）")
    @PostMapping("/{id}/test")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<Map<String, Object>> testPrinter(@PathVariable("id") Long id) {
        return printerService.testPrinter(id);
    }

    /**
     * TC-12.2.3.x - 查询可用同类型打印机（前端打印入口）
     * <p>权限：任意已登录用户（12.4 共用前端打印入口）
     */
    @Operation(summary = "查询可用同类型打印机（前端打印入口用）")
    @GetMapping("/available")
    public Result<Map<String, Object>> getAvailablePrinters(
            @RequestParam String type,
            @RequestParam(defaultValue = "1") Long tenantId) {
        return printerService.getAvailablePrinters(type, tenantId);
    }
}
