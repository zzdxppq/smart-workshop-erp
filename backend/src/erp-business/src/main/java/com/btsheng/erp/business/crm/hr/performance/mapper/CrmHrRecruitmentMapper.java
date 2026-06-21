package com.btsheng.erp.business.crm.hr.performance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.hr.performance.entity.CrmHrRecruitment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmHrRecruitmentMapper extends BaseMapper<CrmHrRecruitment> {

    @Select("SELECT * FROM crm_hr_recruitment WHERE recruitment_no = #{recruitmentNo} LIMIT 1")
    CrmHrRecruitment selectByRecruitmentNo(@Param("recruitmentNo") String recruitmentNo);

    @Select("SELECT * FROM crm_hr_recruitment WHERE id = #{id} LIMIT 1")
    CrmHrRecruitment selectById(@Param("id") Long id);

    @Select("SELECT * FROM crm_hr_recruitment " +
            "WHERE (#{finalStatus} IS NULL OR final_status = #{finalStatus}) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectRecruitments(@Param("finalStatus") String finalStatus,
                                                 @Param("limit") int limit,
                                                 @Param("offset") int offset);
}
