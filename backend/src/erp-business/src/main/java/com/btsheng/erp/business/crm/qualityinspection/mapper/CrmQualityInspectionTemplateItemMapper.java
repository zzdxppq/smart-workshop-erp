package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityInspectionTemplateItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityInspectionTemplateItemMapper extends BaseMapper<CrmQualityInspectionTemplateItem> {

    @Select("""
            SELECT * FROM crm_quality_inspection_template_item
            WHERE template_id = #{templateId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<CrmQualityInspectionTemplateItem> selectByTemplateId(@Param("templateId") Long templateId);

    @Delete("DELETE FROM crm_quality_inspection_template_item WHERE template_id = #{templateId}")
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
