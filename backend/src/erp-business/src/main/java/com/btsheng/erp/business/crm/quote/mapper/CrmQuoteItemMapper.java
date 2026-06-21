package com.btsheng.erp.business.crm.quote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQuoteItemMapper extends BaseMapper<CrmQuoteItem> {

    @Select("SELECT * FROM crm_quote_item WHERE quote_id = #{quoteId} ORDER BY sort ASC, id ASC")
    List<CrmQuoteItem> selectByQuoteId(@Param("quoteId") Long quoteId);
}
