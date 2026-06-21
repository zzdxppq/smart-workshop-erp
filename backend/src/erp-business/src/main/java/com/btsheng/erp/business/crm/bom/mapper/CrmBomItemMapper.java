package com.btsheng.erp.business.crm.bom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.bom.entity.CrmBomItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.9 · BOM 多级树 Mapper
 */
@Mapper
public interface CrmBomItemMapper extends BaseMapper<CrmBomItem> {

    @Select("SELECT * FROM crm_bom_item WHERE bom_id = #{bomId} ORDER BY item_level, item_no")
    List<CrmBomItem> selectByBomId(Long bomId);

    @Select("SELECT * FROM crm_bom_item WHERE parent_item_id = #{parentItemId} ORDER BY item_no")
    List<CrmBomItem> selectByParentItemId(Long parentItemId);

    @Select("SELECT segment, SUM(total_cost) AS total FROM crm_bom_item WHERE bom_id = #{bomId} GROUP BY segment")
    List<Map<String, Object>> aggregateBySegment(Long bomId);

    @Select("SELECT MAX(item_level) FROM crm_bom_item WHERE bom_id = #{bomId}")
    Integer maxItemLevel(Long bomId);
}
