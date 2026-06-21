package com.btsheng.erp.business.crm.quote.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quote.template.entity.CrmQuoteTemplateProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V2.1 · 报价范本工序 Mapper
 */
@Mapper
public interface CrmQuoteTemplateProcessMapper extends BaseMapper<CrmQuoteTemplateProcess> {

    @Select("SELECT * FROM crm_quote_template_process WHERE template_id = #{templateId} ORDER BY sequence")
    List<CrmQuoteTemplateProcess> selectByTemplateId(@Param("templateId") Long templateId);
}
