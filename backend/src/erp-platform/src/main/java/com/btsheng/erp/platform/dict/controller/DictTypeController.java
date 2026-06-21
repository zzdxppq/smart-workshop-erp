package com.btsheng.erp.platform.dict.controller;

import com.btsheng.erp.core.dict.entity.DictType;
import com.btsheng.erp.platform.dict.service.DictService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 字典分类 CRUD（V1.3.9 新增）
 * 路径 /dict-types 与 /dicts 解耦，避免与 /dicts/types GET 冲突
 */
@Tag(name = "E1-DictType", description = "字典分类管理")
@RestController
@RequestMapping("/dict-types")
public class DictTypeController {

    private final DictService dictService;

    @Autowired
    public DictTypeController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "创建字典分类")
    @PostMapping
    public Result<DictType> createType(@RequestBody DictType type) {
        return dictService.createType(type);
    }

    @Operation(summary = "更新字典分类")
    @PutMapping("/{typeCode}")
    public Result<DictType> updateType(@PathVariable String typeCode, @RequestBody DictType type) {
        return dictService.updateType(typeCode, type);
    }

    @Operation(summary = "删除字典分类（含所有字典项）")
    @DeleteMapping("/{typeCode}")
    public Result<Void> deleteType(@PathVariable String typeCode) {
        return dictService.deleteType(typeCode);
    }
}
