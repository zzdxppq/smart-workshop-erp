package com.btsheng.erp.platform.sysparam.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.sysparam.entity.ChangeLog;
import com.btsheng.erp.platform.sysparam.mapper.ChangeLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** V1.3.7 Story 1.3 · 变更日志（1 端点） */
@Tag(name = "E1-ChangeLog", description = "变更日志")
@RestController
@RequestMapping("/changelogs")
public class ChangeLogController {

    private final ChangeLogMapper changeLogMapper;

    @Autowired
    public ChangeLogController(ChangeLogMapper changeLogMapper) { this.changeLogMapper = changeLogMapper; }

    @Operation(summary = "查询变更日志")
    @GetMapping
    public Result<List<ChangeLog>> listByEntity(@RequestParam(value = "entity", required = false) String entity) {
        List<ChangeLog> list = entity == null
            ? changeLogMapper.selectList(null)
            : changeLogMapper.selectByEntity(entity);
        return Result.ok(list);
    }
}
