package com.btsheng.erp.platform.dict.controller;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.core.dict.entity.DictType;
import com.btsheng.erp.platform.dict.service.DictService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** V1.3.7 Story 1.3 · AC-1.3.1 · 数据字典 CRUD（5 端点） */
@Tag(name = "E1-Dict", description = "数据字典")
@RestController
@RequestMapping("/dicts")
public class DictController {

    private final DictService dictService;

    @Autowired
    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "查询字典分类列表（V1.3.9 新增）")
    @GetMapping("/types")
    public Result<List<DictType>> listTypes() {
        return dictService.listTypes();
    }

    @Operation(summary = "按类型查询字典列表（可选分页）")
    @GetMapping
    public Result<?> listByType(@RequestParam(value = "type", required = false) String type,
                                @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (pageNum != null && pageSize != null) {
            return dictService.pageByType(type, pageNum, pageSize);
        }
        return dictService.listByType(type);
    }

    @Operation(summary = "查询单个字典")
    @GetMapping("/{id}")
    public Result<Dict> getById(@PathVariable("id") Long id) {
        return dictService.getById(id);
    }

    @Operation(summary = "创建字典项")
    @PostMapping
    public Result<Dict> createDict(@RequestBody Dict dict) {
        return dictService.createDict(dict);
    }

    @Operation(summary = "修改字典项")
    @PutMapping("/{id}")
    public Result<Dict> updateDict(@PathVariable("id") Long id, @RequestBody Dict dict) {
        return dictService.updateDict(id, dict);
    }

    @Operation(summary = "软删字典项")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDict(@PathVariable("id") Long id) {
        return dictService.deleteDict(id);
    }
}
