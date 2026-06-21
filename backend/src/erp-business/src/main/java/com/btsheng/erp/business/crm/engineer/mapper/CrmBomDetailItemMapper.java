package com.btsheng.erp.business.crm.engineer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.engineer.entity.CrmBomDetailItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · BOM子件明细 Mapper
 */
@Mapper
public interface CrmBomDetailItemMapper extends BaseMapper<CrmBomDetailItem> {

    @Select("SELECT * FROM crm_bom_detail WHERE workbench_id = #{workbenchId} ORDER BY sequence")
    List<CrmBomDetailItem> selectByWorkbenchId(@Param("workbenchId") Long workbenchId);
}
