package com.btsheng.erp.business.crm.drawing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.7 · 图纸版本 Mapper
 */
@Mapper
public interface CrmDrawingVersionMapper extends BaseMapper<CrmDrawingVersion> {

    @Select("SELECT * FROM crm_drawing_version WHERE drawing_id = #{drawingId} ORDER BY version ASC")
    List<CrmDrawingVersion> selectByDrawingId(@Param("drawingId") Long drawingId);

    @Select("SELECT MAX(version) FROM crm_drawing_version WHERE drawing_id = #{drawingId}")
    String selectMaxVersion(@Param("drawingId") Long drawingId);
}
