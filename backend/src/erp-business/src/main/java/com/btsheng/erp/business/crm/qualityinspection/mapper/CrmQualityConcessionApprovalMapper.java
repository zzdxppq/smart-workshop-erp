package com.btsheng.erp.business.crm.qualityinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityinspection.entity.CrmQualityConcessionApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityConcessionApprovalMapper extends BaseMapper<CrmQualityConcessionApproval> {

    @Select("SELECT * FROM crm_quality_concession_approval WHERE inspection_id = #{inspectionId}")
    List<CrmQualityConcessionApproval> selectByInspectionId(@Param("inspectionId") Long inspectionId);

    @Select("""
            SELECT * FROM crm_quality_concession_approval
            WHERE inspection_id = #{inspectionId} AND approver_role = #{approverRole}
            LIMIT 1
            """)
    CrmQualityConcessionApproval selectByInspectionAndRole(@Param("inspectionId") Long inspectionId,
                                                           @Param("approverRole") String approverRole);
}
