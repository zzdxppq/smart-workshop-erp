package com.btsheng.erp.business.crm.conversion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.8 · 标注历史 Mapper
 */
@Mapper
public interface CrmDrawingAnnotationHistoryMapper extends BaseMapper<CrmDrawingAnnotationHistory> {

    @Select("SELECT * FROM crm_drawing_annotation_history WHERE annotation_id = #{annotationId} ORDER BY created_at DESC")
    List<CrmDrawingAnnotationHistory> selectByAnnotationId(Long annotationId);

    @Select("SELECT * FROM crm_drawing_annotation_history WHERE drawing_id = #{drawingId} ORDER BY created_at DESC")
    List<CrmDrawingAnnotationHistory> selectByDrawingId(Long drawingId);
}
