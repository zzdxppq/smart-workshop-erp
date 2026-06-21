package com.btsheng.erp.business.crm.conversion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.1 标注 Mapper
 *
 * 8 方法：selectById / selectByDrawingAndVersion / selectByDrawing / selectByType / countByType / archiveOldVersion / countByCreator
 */
@Mapper
public interface CrmDrawingAnnotationMapper extends BaseMapper<CrmDrawingAnnotation> {

    @Select("SELECT * FROM crm_drawing_annotation WHERE drawing_id = #{drawingId} AND version = #{version} AND is_archived = 0 ORDER BY priority DESC, created_at DESC")
    List<CrmDrawingAnnotation> selectByDrawingAndVersion(Long drawingId, String version);

    @Select("SELECT * FROM crm_drawing_annotation WHERE drawing_id = #{drawingId} ORDER BY version DESC, priority DESC")
    List<CrmDrawingAnnotation> selectByDrawing(Long drawingId);

    @Select("SELECT * FROM crm_drawing_annotation WHERE drawing_id = #{drawingId} AND version = #{version} AND type = #{type} AND x = #{x} AND y = #{y} LIMIT 1")
    CrmDrawingAnnotation selectByPosition(Long drawingId, String version, String type, java.math.BigDecimal x, java.math.BigDecimal y);

    @Select("SELECT type, COUNT(*) AS cnt FROM crm_drawing_annotation WHERE drawing_id = #{drawingId} GROUP BY type")
    List<Map<String, Object>> countByType(Long drawingId);

    @Update("UPDATE crm_drawing_annotation SET is_archived = 1 WHERE drawing_id = #{drawingId} AND version = #{oldVersion}")
    int archiveOldVersion(Long drawingId, String oldVersion);

    @Select("SELECT COUNT(*) FROM crm_drawing_annotation WHERE created_by = #{userId} AND DATE(created_at) = #{date}")
    int countByCreatorAndDate(Long userId, String date);
}
