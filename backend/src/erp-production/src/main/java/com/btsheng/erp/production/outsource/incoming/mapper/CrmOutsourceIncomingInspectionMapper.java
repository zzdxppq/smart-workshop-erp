package com.btsheng.erp.production.outsource.incoming.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.incoming.entity.CrmOutsourceIncomingInspection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceIncomingInspectionMapper extends BaseMapper<CrmOutsourceIncomingInspection> {
    @Select("SELECT * FROM crm_outsource_incoming_inspection WHERE outsource_id = #{outsourceId} ORDER BY created_at DESC")
    List<CrmOutsourceIncomingInspection> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_outsource_incoming_inspection WHERE result = #{result} ORDER BY created_at DESC")
    List<CrmOutsourceIncomingInspection> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_outsource_incoming_inspection WHERE inspection_no = #{inspectionNo}")
    CrmOutsourceIncomingInspection selectByInspectionNo(@Param("inspectionNo") String inspectionNo);
}
