package com.btsheng.erp.business.crm.drawing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmDrawingAttachmentMapper extends BaseMapper<CrmDrawingAttachment> {

    @Select("SELECT * FROM crm_drawing_attachment WHERE drawing_id = #{drawingId} ORDER BY created_at DESC")
    List<CrmDrawingAttachment> selectByDrawingId(@Param("drawingId") Long drawingId);
}
