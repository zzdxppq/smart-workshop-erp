package com.btsheng.erp.platform.sysparam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.sysparam.entity.ChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChangeLogMapper extends BaseMapper<ChangeLog> {

    @Select("SELECT * FROM sys_change_log WHERE entity = #{entity} ORDER BY changed_at DESC")
    List<ChangeLog> selectByEntity(String entity);
}
