package com.btsheng.erp.business.crm.engineer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.engineer.entity.CrmProcessDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 工艺明细 Mapper
 */
@Mapper
public interface CrmProcessDetailMapper extends BaseMapper<CrmProcessDetail> {

    @Select("SELECT * FROM crm_process_detail WHERE workbench_id = #{workbenchId} ORDER BY sequence")
    List<CrmProcessDetail> selectByWorkbenchId(@Param("workbenchId") Long workbenchId);
}
