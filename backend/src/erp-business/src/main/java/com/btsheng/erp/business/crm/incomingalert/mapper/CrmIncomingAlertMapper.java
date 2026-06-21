package com.btsheng.erp.business.crm.incomingalert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncomingAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CrmIncomingAlertMapper extends BaseMapper<CrmIncomingAlert> {

    @Select("SELECT * FROM crm_incoming_alert WHERE alert_level = #{alertLevel} ORDER BY expected_date ASC")
    List<CrmIncomingAlert> selectByAlertLevel(@Param("alertLevel") String alertLevel);

    @Select("SELECT * FROM crm_incoming_alert WHERE expected_date <= #{date} AND alert_level IN ('PENDING', 'ALERT') ORDER BY expected_date ASC")
    List<CrmIncomingAlert> selectOverdueOrDueSoon(@Param("date") LocalDate date);

    @Select("SELECT * FROM crm_incoming_alert WHERE po_id = #{poId} AND material_id = #{materialId}")
    CrmIncomingAlert selectByPoAndMaterial(@Param("poId") Long poId, @Param("materialId") Long materialId);

    @Select("SELECT * FROM crm_incoming_alert WHERE alert_level != 'ARRIVED' ORDER BY expected_date ASC")
    List<CrmIncomingAlert> selectPendingAll();
}
