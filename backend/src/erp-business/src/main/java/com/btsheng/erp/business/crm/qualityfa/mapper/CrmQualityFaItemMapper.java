package com.btsheng.erp.business.crm.qualityfa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.qualityfa.entity.CrmQualityFaItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQualityFaItemMapper extends BaseMapper<CrmQualityFaItem> {

    @Select("SELECT * FROM crm_quality_fa_item WHERE fa_id = #{faId}")
    List<CrmQualityFaItem> selectByFaId(@Param("faId") Long faId);
}
