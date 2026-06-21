package com.btsheng.erp.production.outsource.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQuality;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceQualityMapper extends BaseMapper<CrmOutsourceQuality> {
    @Select("SELECT * FROM crm_outsource_quality WHERE outsource_id = #{outsourceId} ORDER BY created_at DESC")
    List<CrmOutsourceQuality> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_outsource_quality WHERE process_name = #{processName} ORDER BY created_at DESC")
    List<CrmOutsourceQuality> selectByProcess(@Param("processName") String processName);

    @Select("SELECT * FROM crm_outsource_quality WHERE result = #{result} ORDER BY created_at DESC")
    List<CrmOutsourceQuality> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_outsource_quality WHERE quality_no = #{qualityNo}")
    CrmOutsourceQuality selectByQualityNo(@Param("qualityNo") String qualityNo);
}
