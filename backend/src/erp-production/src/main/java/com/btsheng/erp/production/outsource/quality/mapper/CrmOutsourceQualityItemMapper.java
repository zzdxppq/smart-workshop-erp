package com.btsheng.erp.production.outsource.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.quality.entity.CrmOutsourceQualityItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceQualityItemMapper extends BaseMapper<CrmOutsourceQualityItem> {
    @Select("SELECT * FROM crm_outsource_quality_item WHERE quality_id = #{qualityId}")
    List<CrmOutsourceQualityItem> selectByQualityId(@Param("qualityId") Long qualityId);

    @Select("SELECT COUNT(*) FROM crm_outsource_quality_item WHERE quality_id = #{qualityId}")
    int countByQualityId(@Param("qualityId") Long qualityId);
}
