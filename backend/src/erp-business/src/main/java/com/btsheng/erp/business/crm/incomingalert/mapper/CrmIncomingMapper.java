package com.btsheng.erp.business.crm.incomingalert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncoming;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmIncomingMapper extends BaseMapper<CrmIncoming> {

    @Select("SELECT * FROM crm_incoming WHERE alert_id = #{alertId} ORDER BY arrived_at DESC")
    List<CrmIncoming> selectByAlertId(@Param("alertId") Long alertId);

    @Select("SELECT * FROM crm_incoming WHERE po_id = #{poId} ORDER BY arrived_at DESC")
    List<CrmIncoming> selectByPoId(@Param("poId") Long poId);
}
