package com.btsheng.erp.business.finance.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPaymentPlanMapper extends BaseMapper<CrmPaymentPlan> {

    @Select("SELECT * FROM crm_payment_plan WHERE order_id = #{orderId}")
    CrmPaymentPlan selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM crm_payment_plan WHERE alert_level != 'PAID' ORDER BY planned_date ASC")
    List<CrmPaymentPlan> selectPending();

    @Select("SELECT * FROM crm_payment_plan WHERE alert_level = 'ALERT_CRITICAL' ORDER BY planned_date ASC")
    List<CrmPaymentPlan> selectOverdue();

    @Select("SELECT * FROM crm_payment_plan ORDER BY planned_date ASC")
    List<CrmPaymentPlan> selectAll();
}
