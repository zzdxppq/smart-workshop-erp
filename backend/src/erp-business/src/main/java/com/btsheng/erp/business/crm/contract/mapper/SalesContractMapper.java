package com.btsheng.erp.business.crm.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.contract.entity.SalesContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SalesContractMapper extends BaseMapper<SalesContract> {

    @Select("SELECT * FROM sales_contract WHERE order_id = #{orderId} LIMIT 1")
    SalesContract selectByOrderId(@Param("orderId") Long orderId);
}
