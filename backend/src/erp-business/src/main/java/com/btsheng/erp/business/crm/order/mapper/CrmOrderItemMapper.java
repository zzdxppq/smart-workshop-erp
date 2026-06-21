package com.btsheng.erp.business.crm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOrderItemMapper extends BaseMapper<CrmOrderItem> {

    @Select("SELECT * FROM crm_order_item WHERE order_id = #{orderId} ORDER BY sort ASC, id ASC")
    List<CrmOrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
