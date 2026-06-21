package com.btsheng.erp.business.finance.receivable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPaymentMapper extends BaseMapper<CrmPayment> {

    @Select("SELECT * FROM crm_payment WHERE ref_id = #{refId} AND type = 'RECEIPT' ORDER BY paid_at DESC")
    List<CrmPayment> selectReceiptsByRefId(@Param("refId") Long refId);

    @Select("SELECT * FROM crm_payment ORDER BY paid_at DESC")
    List<CrmPayment> selectAll();
}
