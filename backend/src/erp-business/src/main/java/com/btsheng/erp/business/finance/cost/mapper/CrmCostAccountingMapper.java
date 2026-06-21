package com.btsheng.erp.business.finance.cost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.cost.entity.CrmCostAccounting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmCostAccountingMapper extends BaseMapper<CrmCostAccounting> {

    @Select("SELECT * FROM crm_cost_accounting WHERE ref_type = #{refType} AND ref_id = #{refId}")
    CrmCostAccounting selectByRef(@Param("refType") String refType, @Param("refId") Long refId);

    @Select("SELECT * FROM crm_cost_accounting WHERE ref_type = #{refType} ORDER BY cost_date DESC")
    List<CrmCostAccounting> selectByRefType(@Param("refType") String refType);

    @Select("SELECT * FROM crm_cost_accounting ORDER BY cost_date DESC")
    List<CrmCostAccounting> selectAll();
}
