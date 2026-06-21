package com.btsheng.erp.business.crm.qualityfa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFa;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityFaMapper extends BaseMapper<CrmQualityFa> {

    @Select("SELECT * FROM crm_quality_fa WHERE work_order_id = #{workOrderId} ORDER BY created_at DESC")
    List<CrmQualityFa> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Select("SELECT * FROM crm_quality_fa WHERE process_id = #{processId} ORDER BY created_at DESC")
    List<CrmQualityFa> selectByProcessId(@Param("processId") Long processId);

    @Select("SELECT * FROM crm_quality_fa WHERE result = #{result} ORDER BY created_at DESC")
    List<CrmQualityFa> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_quality_fa WHERE fa_no = #{faNo}")
    CrmQualityFa selectByFaNo(@Param("faNo") String faNo);
}
