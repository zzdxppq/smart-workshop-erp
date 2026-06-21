package com.btsheng.erp.business.crm.conversion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.conversion.entity.CrmDrawingConversion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.2 工程转化 Mapper
 *
 * 6 方法：selectById / selectByDrawingIdAndVersion / selectByDrawingId / selectList
 */
@Mapper
public interface CrmDrawingConversionMapper extends BaseMapper<CrmDrawingConversion> {

    @Select("SELECT * FROM crm_drawing_conversion WHERE drawing_id = #{drawingId} AND locked_version = #{lockedVersion} LIMIT 1")
    CrmDrawingConversion selectByDrawingIdAndVersion(Long drawingId, String lockedVersion);

    @Select("SELECT * FROM crm_drawing_conversion WHERE drawing_id = #{drawingId} ORDER BY created_at DESC")
    List<CrmDrawingConversion> selectByDrawingId(Long drawingId);

    @Select("SELECT * FROM crm_drawing_conversion WHERE bom_no = #{bomNo} LIMIT 1")
    CrmDrawingConversion selectByBomNo(String bomNo);

    @Select("SELECT status, COUNT(*) AS cnt FROM crm_drawing_conversion GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT * FROM crm_drawing_conversion WHERE status = 'CONVERTED' ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<CrmDrawingConversion> selectConvertedList(int limit, int offset);
}
