package com.btsheng.erp.business.finance.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPaymentAlertMapper extends BaseMapper<CrmPaymentAlert> {

    @Select("SELECT * FROM crm_payment_alert WHERE plan_id = #{planId} ORDER BY created_at DESC")
    List<CrmPaymentAlert> selectByPlanId(@Param("planId") Long planId);

    @Select("SELECT * FROM crm_payment_alert ORDER BY created_at DESC")
    List<CrmPaymentAlert> selectAll();
}
