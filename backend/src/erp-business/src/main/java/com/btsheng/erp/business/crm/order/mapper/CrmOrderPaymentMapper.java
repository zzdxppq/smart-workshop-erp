package com.btsheng.erp.business.crm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrderPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOrderPaymentMapper extends BaseMapper<CrmOrderPayment> {

    @Select("SELECT * FROM crm_order_payment WHERE order_id = #{orderId} ORDER BY payment_date ASC, id ASC")
    List<CrmOrderPayment> selectByOrderId(@Param("orderId") Long orderId);
}
