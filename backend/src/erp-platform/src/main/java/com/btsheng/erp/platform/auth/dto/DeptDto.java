package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "部门")
public class DeptDto {

    private Long id;
    private Long parentId;
    private String deptName;
    private Integer sort;
    private String status;
    private List<DeptDto> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<DeptDto> getChildren() { return children; }
    public void setChildren(List<DeptDto> children) { this.children = children; }
}
