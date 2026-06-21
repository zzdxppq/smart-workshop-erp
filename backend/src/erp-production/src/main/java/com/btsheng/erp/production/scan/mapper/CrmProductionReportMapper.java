package com.btsheng.erp.production.scan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.scan.entity.CrmProductionReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmProductionReportMapper extends BaseMapper<CrmProductionReport> {
    @Select("SELECT * FROM crm_production_report WHERE workorder_no = #{wo} ORDER BY reported_at DESC")
    List<CrmProductionReport> selectByWorkorder(@Param("wo") String workorderNo);

    @Select("SELECT COALESCE(SUM(reported_qty), 0) FROM crm_production_report WHERE workorder_no = #{wo} AND step_no = #{stepNo}")
    int sumReportedQty(@Param("wo") String workorderNo, @Param("stepNo") int stepNo);
}
