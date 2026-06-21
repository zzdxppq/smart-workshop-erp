package com.btsheng.erp.production.rework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.rework.entity.CrmReworkAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmReworkAlertMapper extends BaseMapper<CrmReworkAlert> {
    @Select("SELECT * FROM crm_rework_alert WHERE outsource_id = #{outsourceId} ORDER BY alerted_at DESC")
    List<CrmReworkAlert> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_rework_alert WHERE alert_level IN ('WARN','CRITICAL','EXCEED') ORDER BY alerted_at DESC")
    List<CrmReworkAlert> selectOpenAlerts();

    @Select("SELECT * FROM crm_rework_alert WHERE outsource_id = #{outsourceId} ORDER BY alerted_at DESC LIMIT 1")
    CrmReworkAlert selectLatestByOutsourceId(@Param("outsourceId") Long outsourceId);
}
