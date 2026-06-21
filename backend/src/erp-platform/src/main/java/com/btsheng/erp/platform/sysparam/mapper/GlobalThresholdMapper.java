package com.btsheng.erp.platform.sysparam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.sysparam.entity.GlobalThreshold;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GlobalThresholdMapper extends BaseMapper<GlobalThreshold> {

    @Select("SELECT * FROM sys_global_threshold WHERE biz_type = #{bizType} AND role_code = #{roleCode}")
    GlobalThreshold selectByBizTypeAndRole(@Param("bizType") String bizType, @Param("roleCode") String roleCode);
}
