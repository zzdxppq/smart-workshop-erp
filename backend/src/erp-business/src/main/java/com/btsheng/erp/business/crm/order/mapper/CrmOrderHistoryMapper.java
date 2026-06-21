package com.btsheng.erp.business.crm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrderHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOrderHistoryMapper extends BaseMapper<CrmOrderHistory> {

    @Select("SELECT * FROM crm_order_history WHERE order_id = #{orderId} ORDER BY changed_at ASC, id ASC")
    List<CrmOrderHistory> selectByOrderId(@Param("orderId") Long orderId);
}
