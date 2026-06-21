package com.btsheng.erp.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.dto.DeptDto;
import com.btsheng.erp.platform.auth.dto.DeptSaveRequest;
import com.btsheng.erp.platform.auth.entity.SysDept;
import com.btsheng.erp.platform.auth.mapper.SysDeptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 部门管理（PRD FR-1-1-2 · 树形 parent_id） */
@Service
public class DeptService {

    private final SysDeptMapper deptMapper;

    @Autowired
    public DeptService(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    public Result<List<DeptDto>> listFlat(String status) {
        QueryWrapper<SysDept> qw = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq("status", status.trim());
        }
        qw.orderByAsc("sort").orderByAsc("id");
        List<SysDept> rows = deptMapper.selectList(qw);
        List<DeptDto> out = new ArrayList<>();
        for (SysDept d : rows) {
            out.add(toDto(d));
        }
        return Result.ok(out);
    }

    public Result<List<DeptDto>> listTree(String status) {
        Result<List<DeptDto>> flatRes = listFlat(status);
        if (!flatRes.isSuccess()) {
            return flatRes;
        }
        return Result.ok(buildTree(flatRes.getData()));
    }

    public Result<DeptDto> getById(Long id) {
        SysDept d = deptMapper.selectById(id);
        if (d == null) {
            return Result.fail(40401, "DEPT_NOT_FOUND");
        }
        return Result.ok(toDto(d));
    }

    public Result<DeptDto> create(DeptSaveRequest req) {
        if (req.getDeptName() == null || req.getDeptName().isBlank()) {
            return Result.fail(40001, "DEPT_NAME_REQUIRED");
        }
        if (req.getParentId() != null) {
            SysDept parent = deptMapper.selectById(req.getParentId());
            if (parent == null || "INACTIVE".equals(parent.getStatus())) {
                return Result.fail(40401, "PARENT_DEPT_NOT_FOUND");
            }
        }
        SysDept d = new SysDept();
        d.setParentId(req.getParentId());
        d.setDeptName(req.getDeptName().trim());
        d.setSort(req.getSort() != null ? req.getSort() : 0);
        d.setStatus(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus() : "ACTIVE");
        deptMapper.insert(d);
        return Result.ok(toDto(d));
    }

    public Result<DeptDto> update(Long id, DeptSaveRequest req) {
        SysDept existing = deptMapper.selectById(id);
        if (existing == null || "INACTIVE".equals(existing.getStatus())) {
            return Result.fail(40401, "DEPT_NOT_FOUND");
        }
        if (req.getParentId() != null && req.getParentId().equals(id)) {
            return Result.fail(40001, "DEPT_PARENT_SELF");
        }
        if (req.getParentId() != null) {
            SysDept parent = deptMapper.selectById(req.getParentId());
            if (parent == null || "INACTIVE".equals(parent.getStatus())) {
                return Result.fail(40401, "PARENT_DEPT_NOT_FOUND");
            }
        }
        if (req.getDeptName() != null && !req.getDeptName().isBlank()) {
            existing.setDeptName(req.getDeptName().trim());
        }
        if (req.getParentId() != null) {
            existing.setParentId(req.getParentId());
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            existing.setStatus(req.getStatus());
        }
        deptMapper.updateById(existing);
        return Result.ok(toDto(existing));
    }

    public Result<Void> disable(Long id) {
        SysDept existing = deptMapper.selectById(id);
        if (existing == null || "INACTIVE".equals(existing.getStatus())) {
            return Result.fail(40401, "DEPT_NOT_FOUND");
        }
        if (deptMapper.countActiveChildren(id) > 0) {
            return Result.fail(40903, "DEPT_HAS_CHILDREN");
        }
        if (deptMapper.countActiveUsers(id) > 0) {
            return Result.fail(40903, "DEPT_HAS_USERS");
        }
        existing.setStatus("INACTIVE");
        deptMapper.updateById(existing);
        return Result.ok(null);
    }

    private static DeptDto toDto(SysDept d) {
        DeptDto dto = new DeptDto();
        dto.setId(d.getId());
        dto.setParentId(d.getParentId());
        dto.setDeptName(d.getDeptName());
        dto.setSort(d.getSort());
        dto.setStatus(d.getStatus());
        return dto;
    }

    private static List<DeptDto> buildTree(List<DeptDto> flat) {
        Map<Long, DeptDto> byId = new HashMap<>();
        for (DeptDto d : flat) {
            byId.put(d.getId(), d);
        }
        List<DeptDto> roots = new ArrayList<>();
        for (DeptDto d : flat) {
            if (d.getParentId() == null || !byId.containsKey(d.getParentId())) {
                roots.add(d);
            } else {
                byId.get(d.getParentId()).getChildren().add(d);
            }
        }
        sortTree(roots);
        return roots;
    }

    private static void sortTree(List<DeptDto> nodes) {
        nodes.sort(Comparator.comparing(DeptDto::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(DeptDto::getId, Comparator.nullsLast(Long::compareTo)));
        for (DeptDto n : nodes) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortTree(n.getChildren());
            }
        }
    }
}
