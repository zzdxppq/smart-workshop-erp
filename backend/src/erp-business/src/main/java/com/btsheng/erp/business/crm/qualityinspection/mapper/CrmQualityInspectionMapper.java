package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityInspectionMapper extends BaseMapper<CrmQualityInspection> {

    @Select("SELECT * FROM crm_quality_inspection WHERE inspect_type = #{inspectType} ORDER BY created_at DESC")
    List<CrmQualityInspection> selectByInspectType(@Param("inspectType") String inspectType);

    @Select("SELECT * FROM crm_quality_inspection WHERE material_id = #{materialId} ORDER BY created_at DESC")
    List<CrmQualityInspection> selectByMaterialId(@Param("materialId") Long materialId);

    @Select("SELECT * FROM crm_quality_inspection WHERE work_order_id = #{workOrderId} ORDER BY created_at DESC")
    List<CrmQualityInspection> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Select("SELECT * FROM crm_quality_inspection WHERE result = #{result} ORDER BY created_at DESC")
    List<CrmQualityInspection> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_quality_inspection WHERE inspection_no = #{inspectionNo}")
    CrmQualityInspection selectByInspectionNo(@Param("inspectionNo") String inspectionNo);

    @Select("SELECT * FROM crm_quality_inspection WHERE source_ref = #{sourceRef} LIMIT 1")
    CrmQualityInspection selectBySourceRef(@Param("sourceRef") String sourceRef);
}
