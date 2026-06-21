package com.btsheng.erp.platform.auth.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.ApiLog;
import com.btsheng.erp.platform.auth.dto.DeptDto;
import com.btsheng.erp.platform.auth.dto.DeptSaveRequest;
import com.btsheng.erp.platform.auth.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 部门管理（PRD FR-1-1-2） */
@Tag(name = "E1-Dept", description = "部门管理")
@RestController
@RequestMapping("/depts")
public class DeptController {

    private final DeptService deptService;

    @Autowired
    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @Operation(summary = "部门列表（tree=true 返回树形）")
    @GetMapping
    public Result<List<DeptDto>> list(@RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "false") boolean tree) {
        return tree ? deptService.listTree(status) : deptService.listFlat(status);
    }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public Result<DeptDto> get(@PathVariable Long id) {
        return deptService.getById(id);
    }

    @Operation(summary = "创建部门")
    @PostMapping
    @ApiLog("dept.create")
    public Result<DeptDto> create(@Valid @RequestBody DeptSaveRequest req) {
        return deptService.create(req);
    }

    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    @ApiLog("dept.update")
    public Result<DeptDto> update(@PathVariable Long id, @RequestBody DeptSaveRequest req) {
        return deptService.update(id, req);
    }

    @Operation(summary = "停用部门")
    @DeleteMapping("/{id}")
    @ApiLog("dept.disable")
    public Result<Void> disable(@PathVariable Long id) {
        return deptService.disable(id);
    }
}
