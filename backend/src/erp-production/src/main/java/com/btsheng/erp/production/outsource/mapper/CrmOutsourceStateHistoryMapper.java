package com.btsheng.erp.production.outsource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.22 · 委外状态机历史 Mapper
 */
@Mapper
public interface CrmOutsourceStateHistoryMapper extends BaseMapper<CrmOutsourceStateHistory> {

    @Select("SELECT * FROM crm_outsource_state_history WHERE outsource_id = #{outsourceId} ORDER BY occurred_at ASC, id ASC")
    List<CrmOutsourceStateHistory> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_outsource_state_history WHERE outsource_no = #{outsourceNo} ORDER BY occurred_at ASC, id ASC")
    List<CrmOutsourceStateHistory> selectByOutsourceNo(@Param("outsourceNo") String outsourceNo);
}
