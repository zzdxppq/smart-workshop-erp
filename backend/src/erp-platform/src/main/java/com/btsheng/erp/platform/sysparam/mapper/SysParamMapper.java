package com.btsheng.erp.platform.sysparam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.sysparam.entity.SysParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysParamMapper extends BaseMapper<SysParam> {

    @Select("SELECT * FROM sys_param WHERE param_group = #{group} ORDER BY param_key ASC")
    List<SysParam> selectByGroup(@Param("group") String group);
}
