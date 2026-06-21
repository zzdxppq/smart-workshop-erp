package com.btsheng.erp.business.crm.bom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.bom.entity.CrmBomHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.9 · BOM 历史 Mapper
 */
@Mapper
public interface CrmBomHistoryMapper extends BaseMapper<CrmBomHistory> {

    @Select("SELECT * FROM crm_bom_history WHERE bom_id = #{bomId} ORDER BY changed_at DESC")
    List<CrmBomHistory> selectByBomId(Long bomId);

    @Select("SELECT * FROM crm_bom_history WHERE work_order_no = #{workOrderNo} LIMIT 1")
    CrmBomHistory selectByWorkOrderNo(String workOrderNo);
}
