package com.btsheng.erp.production.outsource.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityDefect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceQualityDefectMapper extends BaseMapper<CrmOutsourceQualityDefect> {
    @Select("SELECT * FROM crm_outsource_quality_defect WHERE quality_id = #{qualityId}")
    List<CrmOutsourceQualityDefect> selectByQualityId(@Param("qualityId") Long qualityId);

    @Select("SELECT COUNT(*) FROM crm_outsource_quality_defect WHERE quality_id = #{qualityId} AND severity = 'CRITICAL'")
    int countCriticalByQualityId(@Param("qualityId") Long qualityId);
}
