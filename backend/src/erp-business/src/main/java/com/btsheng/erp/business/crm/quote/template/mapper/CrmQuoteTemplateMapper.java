package com.btsheng.erp.business.crm.quote.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quote.template.entity.CrmQuoteTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 报价范本 Mapper
 */
@Mapper
public interface CrmQuoteTemplateMapper extends BaseMapper<CrmQuoteTemplate> {

    @Select("SELECT * FROM crm_quote_template WHERE id = #{id} AND is_active = 1 LIMIT 1")
    CrmQuoteTemplate selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM crm_quote_template WHERE is_active = 1 ORDER BY created_at DESC")
    List<CrmQuoteTemplate> selectAllActive();

    @Select("SELECT * FROM crm_quote_template WHERE category = #{category} AND is_active = 1 ORDER BY template_name")
    List<CrmQuoteTemplate> selectByCategory(@Param("category") String category);

    @Select("SELECT * FROM crm_quote_template WHERE process_type = #{processType} AND is_active = 1 ORDER BY template_name")
    List<CrmQuoteTemplate> selectByProcessType(@Param("processType") String processType);
}
