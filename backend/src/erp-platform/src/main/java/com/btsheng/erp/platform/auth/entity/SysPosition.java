package com.btsheng.erp.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.PlatformIdDO;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 职位实体（V1.3.7）
 */
@Schema(description = "职位")
@TableName("sys_position")
public class SysPosition extends PlatformIdDO {

    private static final long serialVersionUID = 1L;

    @TableField("dept_id")
    private Long deptId;

    @TableField("position_name")
    private String positionName;

    @TableField("sort")
    private Integer sort;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
