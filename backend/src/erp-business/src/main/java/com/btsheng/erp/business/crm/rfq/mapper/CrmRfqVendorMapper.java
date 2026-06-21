package com.btsheng.erp.business.crm.rfq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqVendor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmRfqVendorMapper extends BaseMapper<CrmRfqVendor> {

    @Select("SELECT * FROM crm_rfq_vendor WHERE rfq_id = #{rfqId}")
    List<CrmRfqVendor> selectByRfqId(@Param("rfqId") Long rfqId);

    @Select("SELECT * FROM crm_rfq_vendor WHERE rfq_id = #{rfqId} AND vendor_id = #{vendorId}")
    CrmRfqVendor selectByRfqAndVendor(@Param("rfqId") Long rfqId, @Param("vendorId") Long vendorId);

    @Select("SELECT COUNT(*) FROM crm_rfq_vendor WHERE rfq_id = #{rfqId}")
    int countByRfqId(@Param("rfqId") Long rfqId);

    @Select("SELECT vendor_name FROM crm_rfq_vendor WHERE vendor_id = #{vendorId} ORDER BY id DESC LIMIT 1")
    String findVendorNameByVendorId(@Param("vendorId") Long vendorId);
}
