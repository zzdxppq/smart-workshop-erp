package com.btsheng.erp.platform.auth.dto;

import com.btsheng.erp.core.model.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 用户 DTO（V1.3.7）
 *
 * <p>响应体不返回 passwordHash 字段（{@code @JsonIgnore}）。
 */
@Schema(description = "用户 DTO")
public class UserDto extends BaseDTO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID", example = "10086")
    private Long id;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "角色编码列表")
    private List<String> roleCodes;

    @JsonIgnore
    @Schema(hidden = true)
    private String passwordHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
