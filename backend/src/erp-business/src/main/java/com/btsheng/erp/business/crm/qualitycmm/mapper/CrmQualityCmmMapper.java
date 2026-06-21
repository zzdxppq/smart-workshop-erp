package com.btsheng.erp.business.crm.qualitycmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualitycmm.entity.CrmQualityCmm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityCmmMapper extends BaseMapper<CrmQualityCmm> {

    @Select("SELECT * FROM crm_quality_cmm WHERE work_order_id = #{workOrderId} ORDER BY created_at DESC")
    List<CrmQualityCmm> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Select("SELECT * FROM crm_quality_cmm WHERE result = #{result} ORDER BY created_at DESC")
    List<CrmQualityCmm> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_quality_cmm WHERE cmm_no = #{cmmNo}")
    CrmQualityCmm selectByCmmNo(@Param("cmmNo") String cmmNo);
}
