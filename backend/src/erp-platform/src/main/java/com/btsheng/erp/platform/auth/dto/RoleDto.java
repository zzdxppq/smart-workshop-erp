package com.btsheng.erp.platform.auth.dto;

import com.btsheng.erp.core.model.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 角色 DTO（V1.3.7）
 */
@Schema(description = "角色 DTO")
public class RoleDto extends BaseDTO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名")
    private String roleName;

    @Schema(description = "数据范围")
    private String dataScope;

    @Schema(description = "金额阈值")
    private BigDecimal amountThreshold;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "权限菜单")
    private List<PermissionAssignRequest> permissions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public List<PermissionAssignRequest> getPermissions() { return permissions; }
    public void setPermissions(List<PermissionAssignRequest> permissions) { this.permissions = permissions; }
}
