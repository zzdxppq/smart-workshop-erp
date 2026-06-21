package com.btsheng.erp.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.path FROM sys_menu m "
            + "INNER JOIN sys_role_permission rp ON rp.menu_id = m.id "
            + "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND rp.action = 'view' AND m.status = 'ACTIVE' "
            + "ORDER BY m.sort ASC, m.id ASC")
    List<String> selectPathsByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT CONCAT(m.menu_code, ':', rp.action) AS perm "
            + "FROM sys_menu m "
            + "INNER JOIN sys_role_permission rp ON rp.menu_id = m.id "
            + "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND m.status = 'ACTIVE' "
            + "ORDER BY perm")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT path FROM sys_menu WHERE status = 'ACTIVE' ORDER BY sort ASC, id ASC")
    List<String> selectAllActivePaths();

    @Select("SELECT m.id, m.parent_id AS parentId, m.menu_code AS menuCode, m.menu_name AS menuName, "
            + "m.path, m.menu_type AS menuType, m.sort, m.icon "
            + "FROM sys_menu m "
            + "INNER JOIN sys_role_permission rp ON rp.menu_id = m.id "
            + "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND rp.action = 'view' AND m.status = 'ACTIVE' "
            + "ORDER BY m.sort ASC, m.id ASC")
    List<Map<String, Object>> selectMenuTreeByUserId(@Param("userId") Long userId);

    @Select("SELECT m.id, m.parent_id AS parentId, m.menu_code AS menuCode, m.menu_name AS menuName, "
            + "m.path, m.menu_type AS menuType, m.sort, m.icon "
            + "FROM sys_menu m WHERE m.status = 'ACTIVE' ORDER BY m.sort ASC, m.id ASC")
    List<Map<String, Object>> selectAllActiveMenus();
}
