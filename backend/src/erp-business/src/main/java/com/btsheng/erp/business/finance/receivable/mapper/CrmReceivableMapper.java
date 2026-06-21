package com.btsheng.erp.business.finance.receivable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.receivable.entity.CrmReceivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmReceivableMapper extends BaseMapper<CrmReceivable> {

    @Select("SELECT * FROM crm_receivable WHERE order_id = #{orderId}")
    CrmReceivable selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM crm_receivable WHERE status != 'CLOSED' ORDER BY due_date ASC")
    List<CrmReceivable> selectOpen();

    @Select("SELECT * FROM crm_receivable ORDER BY due_date ASC")
    List<CrmReceivable> selectAll();
}
