package com.btsheng.erp.business.internal.productroute.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.internal.productroute.entity.MdmProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MdmProcessMapper extends BaseMapper<MdmProcess> {

    @Select("SELECT * FROM mdm_process WHERE process_code = #{processCode} LIMIT 1")
    MdmProcess selectByCode(String processCode);
}
