package com.btsheng.erp.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND status = 'ACTIVE'")
    long countActiveUsers(@Param("deptId") Long deptId);

    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id = #{deptId} AND status = 'ACTIVE'")
    long countActiveChildren(@Param("deptId") Long deptId);
}
