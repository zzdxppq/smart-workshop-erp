package com.btsheng.erp.business.crm.rfq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmRfqQuoteMapper extends BaseMapper<CrmRfqQuote> {

    @Select("SELECT * FROM crm_rfq_quote WHERE rfq_id = #{rfqId} ORDER BY total_amount ASC")
    List<CrmRfqQuote> selectByRfqId(@Param("rfqId") Long rfqId);

    @Select("SELECT * FROM crm_rfq_quote WHERE rfq_id = #{rfqId} AND vendor_id = #{vendorId}")
    CrmRfqQuote selectByRfqAndVendor(@Param("rfqId") Long rfqId, @Param("vendorId") Long vendorId);

    @Select("SELECT * FROM crm_rfq_quote WHERE is_awarded = 1 AND rfq_id = #{rfqId}")
    CrmRfqQuote selectAwardedByRfqId(@Param("rfqId") Long rfqId);
}
