package com.btsheng.erp.business.crm.rfq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmRfqMapper extends BaseMapper<CrmRfq> {

    @Select("SELECT * FROM crm_rfq WHERE rfq_no = #{rfqNo}")
    CrmRfq selectByRfqNo(@Param("rfqNo") String rfqNo);

    @Select("SELECT * FROM crm_rfq WHERE status = #{status} ORDER BY created_at DESC")
    List<CrmRfq> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM crm_rfq ORDER BY created_at DESC")
    List<CrmRfq> selectListAll();
}
