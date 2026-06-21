package com.btsheng.erp.business.crm.drawing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.7 · 图纸变更历史 Mapper（@AuditLog AFTER_COMMIT 写入）
 */
@Mapper
public interface CrmDrawingHistoryMapper extends BaseMapper<CrmDrawingHistory> {

    @Select("SELECT * FROM crm_drawing_history WHERE drawing_id = #{drawingId} ORDER BY changed_at DESC")
    List<CrmDrawingHistory> selectByDrawingId(@Param("drawingId") Long drawingId);
}
