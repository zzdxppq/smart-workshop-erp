package com.btsheng.erp.business.crm.quote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmQuoteHistoryMapper extends BaseMapper<CrmQuoteHistory> {

    @Select("SELECT * FROM crm_quote_history WHERE quote_id = #{quoteId} ORDER BY changed_at ASC, id ASC")
    List<CrmQuoteHistory> selectByQuoteId(@Param("quoteId") Long quoteId);
}
