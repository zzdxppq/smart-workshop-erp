package com.btsheng.erp.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_permission (role_id, menu_id, action) VALUES (#{roleId}, #{menuId}, #{action})")
    int insertPerm(@Param("roleId") Long roleId, @Param("menuId") Long menuId, @Param("action") String action);

    @Select("SELECT menu_id, action FROM sys_role_permission WHERE role_id = #{roleId}")
    List<SysRolePermission> findByRoleId(@Param("roleId") Long roleId);
}
