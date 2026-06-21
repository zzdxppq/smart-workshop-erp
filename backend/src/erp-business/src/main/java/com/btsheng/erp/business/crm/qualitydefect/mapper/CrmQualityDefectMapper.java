package com.btsheng.erp.business.crm.qualitydefect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityDefectMapper extends BaseMapper<CrmQualityDefect> {

    @Select("SELECT * FROM crm_quality_defect WHERE source_type = #{sourceType} ORDER BY created_at DESC")
    List<CrmQualityDefect> selectBySourceType(@Param("sourceType") String sourceType);

    @Select("SELECT * FROM crm_quality_defect WHERE status = #{status} ORDER BY created_at DESC")
    List<CrmQualityDefect> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM crm_quality_defect WHERE result = #{result} ORDER BY created_at DESC")
    List<CrmQualityDefect> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_quality_defect WHERE defect_no = #{defectNo}")
    CrmQualityDefect selectByDefectNo(@Param("defectNo") String defectNo);
}
