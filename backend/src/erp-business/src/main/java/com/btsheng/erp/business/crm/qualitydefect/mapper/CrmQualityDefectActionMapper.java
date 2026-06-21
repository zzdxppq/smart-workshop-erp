package com.btsheng.erp.business.crm.qualitydefect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualitydefect.entity.CrmQualityDefectAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityDefectActionMapper extends BaseMapper<CrmQualityDefectAction> {

    @Select("SELECT * FROM crm_quality_defect_action WHERE defect_id = #{defectId} ORDER BY created_at")
    List<CrmQualityDefectAction> selectByDefectId(@Param("defectId") Long defectId);
}
