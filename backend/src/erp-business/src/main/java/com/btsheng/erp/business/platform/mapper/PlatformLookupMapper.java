package com.btsheng.erp.business.platform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 跨库查询 cnc_platform 字典与用户（V1.3.9 · 业务服务只读）
 */
@Mapper
public interface PlatformLookupMapper {

    @Select("SELECT id FROM cnc_platform.sys_user " +
            "WHERE LOWER(username) = LOWER(#{username}) AND status = 'ACTIVE' LIMIT 1")
    Long findUserIdByUsername(@Param("username") String username);

    @Select("SELECT username FROM cnc_platform.sys_user WHERE id = #{userId} LIMIT 1")
    String findUsernameByUserId(@Param("userId") Long userId);

    @Select("SELECT dict_label FROM cnc_platform.sys_dict " +
            "WHERE dict_type = 'DRAWING_ACL_FEATURE_FLAG' AND dict_code = #{dictCode} " +
            "AND status != 'DELETED' LIMIT 1")
    String findDrawingGrayFlag(@Param("dictCode") String dictCode);

    @Select("SELECT dept_name FROM cnc_platform.sys_dept " +
            "WHERE id = #{deptId} AND status = 'ACTIVE' LIMIT 1")
    String findDeptNameById(@Param("deptId") Long deptId);

    @Select("SELECT id FROM cnc_platform.sys_dept " +
            "WHERE dept_name = #{deptName} AND status = 'ACTIVE' LIMIT 1")
    Long findDeptIdByName(@Param("deptName") String deptName);

    /**
     * V94 · 校验 dict_code 是否属于指定字典类型（员工岗位等）
     * 仅查 ACTIVE 状态；返回 1=存在，0=不存在
     */
    @Select("SELECT COUNT(*) FROM cnc_platform.sys_dict " +
            "WHERE dict_type = #{dictType} AND dict_code = #{dictCode} " +
            "AND status = 'ACTIVE' LIMIT 1")
    int existsDictCode(@Param("dictType") String dictType, @Param("dictCode") String dictCode);
}
