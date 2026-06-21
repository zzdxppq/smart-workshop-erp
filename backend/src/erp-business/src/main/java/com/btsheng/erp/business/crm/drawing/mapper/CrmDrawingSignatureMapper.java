package com.btsheng.erp.business.crm.drawing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingSignature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.7 · 图纸签字扫描件 Mapper
 */
@Mapper
public interface CrmDrawingSignatureMapper extends BaseMapper<CrmDrawingSignature> {

    @Select("SELECT * FROM crm_drawing_signature WHERE drawing_id = #{drawingId} AND version = #{version}")
    List<CrmDrawingSignature> selectByDrawingIdAndVersion(@Param("drawingId") Long drawingId,
                                                            @Param("version") String version);
}
