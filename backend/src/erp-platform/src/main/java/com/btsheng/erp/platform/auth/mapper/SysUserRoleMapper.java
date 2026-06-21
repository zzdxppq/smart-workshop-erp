package com.btsheng.erp.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertRel(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT r.role_code FROM sys_role r "
            + "INNER JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.status = 'ACTIVE'")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT r.data_scope FROM sys_role r "
            + "INNER JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.status = 'ACTIVE'")
    List<String> findDataScopesByUserId(@Param("userId") Long userId);
}
