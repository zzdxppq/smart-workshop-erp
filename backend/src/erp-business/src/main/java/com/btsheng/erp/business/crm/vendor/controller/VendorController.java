package com.btsheng.erp.business.crm.vendor.controller;

import com.btsheng.erp.business.crm.vendor.dto.CreateVendorRequest;
import com.btsheng.erp.business.crm.vendor.dto.UpdateVendorNotifyRequest;
import com.btsheng.erp.business.crm.vendor.dto.UpdateVendorRequest;
import com.btsheng.erp.business.crm.vendor.service.VendorService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/vendors")
@Tag(name = "E6-Vendors", description = "厂商资料（E6-S8 · Story 1.23）")
public class VendorController {

    private final VendorService service;

    @Autowired
    public VendorController(VendorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "厂商列表")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(keyword, pageNum, pageSize);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'ADMIN', 'SYS_ADMIN')")
    @Operation(summary = "新增厂商（E6-S8 · 采购员维护）")
    public Result<Map<String, Object>> create(@RequestBody CreateVendorRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "厂商详情")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'ADMIN', 'SYS_ADMIN')")
    @Operation(summary = "更新厂商资料")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody UpdateVendorRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/{id}/notify-pref")
    @PreAuthorize("hasAnyRole('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'ADMIN', 'SYS_ADMIN')")
    @Operation(summary = "更新接收邮箱（系统经 163 SMTP 发送，接收方不限 163）")
    public Result<Map<String, Object>> updateNotifyPref(
            @PathVariable Long id,
            @RequestBody UpdateVendorNotifyRequest req) {
        return service.updateNotifyPref(id, req);
    }

    @PostMapping("/{id}/upload-license")
    @PreAuthorize("hasAnyRole('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'ADMIN', 'SYS_ADMIN')")
    @Operation(summary = "上传营业执照（V2.2）")
    public Result<Map<String, Object>> uploadLicense(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return service.uploadBusinessLicense(id, file);
    }
}
