package com.btsheng.erp.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建用户请求（V1.3.7 · AC-1.1.1）
 */
@Schema(description = "创建用户请求")
public class UserCreateRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名仅支持 3-20 位字母数字下划线")
    @Schema(description = "登录名")
    private String username;

    @NotBlank
    @Size(min = 3, max = 32, message = "密码长度 3-32 位")
    @Schema(description = "密码")
    private String password;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "真实姓名")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号必须为 11 位数字")
    @Schema(description = "手机号（AES-256-GCM 加密）")
    private String phone;

    @Email
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门 ID")
    private Long deptId;

    @NotBlank
    @Schema(description = "角色编码数组，至少 1 个", example = "[\"salesperson\"]")
    private List<String> roleCodes;

    @Schema(description = "HR 建档自动开通：允许初始密码与登录名相同（可短于常规强度规则）")
    private Boolean hrAutoProvision;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
    public Boolean getHrAutoProvision() { return hrAutoProvision; }
    public void setHrAutoProvision(Boolean hrAutoProvision) { this.hrAutoProvision = hrAutoProvision; }
}
