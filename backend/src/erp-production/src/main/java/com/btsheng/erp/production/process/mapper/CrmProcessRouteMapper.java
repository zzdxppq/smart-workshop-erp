package com.btsheng.erp.production.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.process.entity.CrmProcessRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.10 · 工艺路线 Mapper
 */
@Mapper
public interface CrmProcessRouteMapper extends BaseMapper<CrmProcessRoute> {

    @Select("SELECT * FROM crm_process_route WHERE drawing_id = #{drawingId} AND version = #{version} LIMIT 1")
    CrmProcessRoute selectByDrawingIdAndVersion(Long drawingId, String version);

    @Select("SELECT * FROM crm_process_route WHERE drawing_id = #{drawingId} ORDER BY version DESC")
    List<CrmProcessRoute> selectByDrawingId(Long drawingId);

    @Select("SELECT * FROM crm_process_route WHERE process_id = #{processId} ORDER BY updated_at DESC LIMIT 1")
    CrmProcessRoute selectLatestByProcessId(Long processId);
}
