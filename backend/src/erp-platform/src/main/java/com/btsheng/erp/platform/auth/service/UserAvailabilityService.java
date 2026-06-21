package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户可用性查询（审批跳过请假 · platform 本地数据）
 *
 * <p>由 erp-business HR 模块通过 Redis Stream 同步 {@code availability_status}，platform 不再反向调用 business。
 */
@Service
public class UserAvailabilityService {

    public static final String AVAIL_ON_DUTY = "ON_DUTY";

    private final SysUserMapper userMapper;

    public UserAvailabilityService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Optional<Availability> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        SysUser user = userMapper.findStatusById(userId);
        if (user == null) {
            return Optional.empty();
        }
        Availability availability = new Availability();
        availability.setUserId(userId);
        availability.setAccountStatus(user.getStatus());
        availability.setAvailabilityStatus(
                user.getAvailabilityStatus() == null ? AVAIL_ON_DUTY : user.getAvailabilityStatus());
        availability.setLeaveNo(user.getLeaveNo());
        return Optional.of(availability);
    }

    public void syncAvailability(Long userId, String availabilityStatus, String leaveNo) {
        if (userId == null) {
            return;
        }
        String status = (availabilityStatus == null || availabilityStatus.isBlank())
                ? AVAIL_ON_DUTY : availabilityStatus.trim();
        userMapper.updateAvailability(userId, status, leaveNo);
    }

    public static class Availability {
        private Long userId;
        private String accountStatus;
        private String availabilityStatus;
        private String leaveNo;

        /** 供 {@code SkipOnLeaveRule} 判定：账号禁用优先，否则看可用性状态 */
        public String effectiveSkipStatus() {
            if ("DISABLED".equalsIgnoreCase(accountStatus)) {
                return "DISABLED";
            }
            return availabilityStatus;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAccountStatus() { return accountStatus; }
        public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
        public String getAvailabilityStatus() { return availabilityStatus; }
        public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
        public String getLeaveNo() { return leaveNo; }
        public void setLeaveNo(String leaveNo) { this.leaveNo = leaveNo; }
    }
}
