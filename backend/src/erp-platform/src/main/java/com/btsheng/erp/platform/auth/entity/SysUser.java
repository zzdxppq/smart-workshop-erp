package com.btsheng.erp.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.PlatformAuditDO;
import com.btsheng.erp.core.web.EncryptedField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户实体（V1.3.7）
 *
 * <p>DB 表 {@code sys_user}（init.sql）。{@code phone} 字段 AES-256-GCM 加密。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "用户")
@TableName("sys_user")
public class SysUser extends PlatformAuditDO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录名", example = "zhangsan")
    @TableField("username")
    private String username;

    @Schema(description = "BCrypt(cost=12) 密文，禁返回前端")
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;

    @Schema(description = "真实姓名")
    @TableField("real_name")
    private String realName;

    @Schema(description = "手机号（AES-256-GCM）")
    @EncryptedField("phone")
    @TableField("phone")
    private String phone;

    @Schema(description = "邮箱")
    @TableField("email")
    private String email;

    @Schema(description = "部门 ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "状态：ACTIVE / DISABLED")
    @TableField("status")
    private String status;

    @Schema(description = "审批可用性：ON_DUTY / ON_LEAVE / ON_TRIP / RESIGNED")
    @TableField("availability_status")
    private String availabilityStatus;

    @Schema(description = "请假单号（同步自 HR）")
    @TableField("leave_no")
    private String leaveNo;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
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
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
    public String getLeaveNo() { return leaveNo; }
    public void setLeaveNo(String leaveNo) { this.leaveNo = leaveNo; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
}
