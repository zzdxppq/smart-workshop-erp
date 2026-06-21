package com.btsheng.erp.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.PlatformAuditDO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 角色实体（V1.3.7）
 *
 * <p>DB 表 {@code sys_role}。金额阈值 {@code amount_threshold}（NULL = 无限额）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "角色")
@TableName("sys_role")
public class SysRole extends PlatformAuditDO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码", example = "salesperson")
    @TableField("role_code")
    private String roleCode;

    @Schema(description = "角色名", example = "业务员")
    @TableField("role_name")
    private String roleName;

    @Schema(description = "数据范围：SELF/DEPT/ALL/CUSTOM")
    @TableField("data_scope")
    private String dataScope;

    @Schema(description = "金额阈值（NULL=无限额）")
    @TableField("amount_threshold")
    private BigDecimal amountThreshold;

    @Schema(description = "状态：ACTIVE/DISABLED")
    @TableField("status")
    private String status;

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public BigDecimal getAmountThreshold() { return amountThreshold; }
    public void setAmountThreshold(BigDecimal amountThreshold) { this.amountThreshold = amountThreshold; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
