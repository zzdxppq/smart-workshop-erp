package com.btsheng.erp.production.outsource.incoming.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceIncomingItemMapper extends BaseMapper<CrmOutsourceIncomingItem> {
    @Select("SELECT * FROM crm_outsource_incoming_item WHERE inspection_id = #{inspectionId}")
    List<CrmOutsourceIncomingItem> selectByInspectionId(@Param("inspectionId") Long inspectionId);

    @Select("SELECT COUNT(*) FROM crm_outsource_incoming_item WHERE inspection_id = #{inspectionId}")
    int countByInspectionId(@Param("inspectionId") Long inspectionId);
}
