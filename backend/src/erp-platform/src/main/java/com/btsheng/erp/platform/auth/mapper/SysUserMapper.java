package com.btsheng.erp.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE LOWER(username) = LOWER(#{username}) AND status = 'ACTIVE' LIMIT 1")
    SysUser findActiveByUsername(String username);

    @Select("SELECT id, status, availability_status, leave_no FROM sys_user WHERE id = #{id}")
    SysUser findStatusById(Long id);

    @Update("UPDATE sys_user SET availability_status = #{availabilityStatus}, leave_no = #{leaveNo}, "
            + "updated_at = NOW() WHERE id = #{userId}")
    int updateAvailability(@Param("userId") Long userId,
                           @Param("availabilityStatus") String availabilityStatus,
                           @Param("leaveNo") String leaveNo);
}
