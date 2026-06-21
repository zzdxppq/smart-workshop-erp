package com.btsheng.erp.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.PlatformIdDO;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 部门实体（V1.3.7）
 *
 * <p>DB 表 {@code sys_dept}（自引用树形）。与角色解耦（FR-1-1-4）。
 */
@Schema(description = "部门")
@TableName("sys_dept")
public class SysDept extends PlatformIdDO {

    private static final long serialVersionUID = 1L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("dept_name")
    private String deptName;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private String status;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
