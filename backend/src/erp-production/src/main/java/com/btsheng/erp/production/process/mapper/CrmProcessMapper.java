package com.btsheng.erp.production.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.process.entity.CrmProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.10 · 工艺库 Mapper
 */
@Mapper
public interface CrmProcessMapper extends BaseMapper<CrmProcess> {

    @Select("SELECT * FROM crm_process WHERE process_code = #{processCode} LIMIT 1")
    CrmProcess selectByProcessCode(String processCode);

    @Select("SELECT * FROM crm_process WHERE drawing_id = #{drawingId} AND is_active = 1")
    List<CrmProcess> selectByDrawingId(Long drawingId);

    @Select("SELECT * FROM crm_process WHERE is_active = 1 ORDER BY created_at DESC")
    List<CrmProcess> selectActive();
}
