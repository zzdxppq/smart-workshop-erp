package com.btsheng.erp.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT * FROM sys_role WHERE role_code = #{code} LIMIT 1")
    SysRole findByCode(String code);

    @Select("SELECT id, role_code, role_name, data_scope, amount_threshold, status FROM sys_role WHERE id = #{id}")
    SysRole findById(Long id);
}
