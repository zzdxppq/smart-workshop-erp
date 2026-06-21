package com.btsheng.erp.production.outsource.incoming.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingDefect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceIncomingDefectMapper extends BaseMapper<CrmOutsourceIncomingDefect> {
    @Select("SELECT * FROM crm_outsource_incoming_defect WHERE inspection_id = #{inspectionId}")
    List<CrmOutsourceIncomingDefect> selectByInspectionId(@Param("inspectionId") Long inspectionId);

    @Select("SELECT COUNT(*) FROM crm_outsource_incoming_defect WHERE inspection_id = #{inspectionId} AND severity = 'CRITICAL'")
    int countCriticalByInspectionId(@Param("inspectionId") Long inspectionId);
}
