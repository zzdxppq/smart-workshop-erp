package com.btsheng.erp.business.finance.receivable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.finance.receivable.entity.CrmPayable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPayableMapper extends BaseMapper<CrmPayable> {

    @Select("SELECT * FROM crm_payable WHERE po_id = #{poId}")
    CrmPayable selectByPoId(@Param("poId") Long poId);

    @Select("SELECT * FROM crm_payable WHERE status != 'CLOSED' ORDER BY due_date ASC")
    List<CrmPayable> selectOpen();

    @Select("SELECT * FROM crm_payable ORDER BY due_date ASC")
    List<CrmPayable> selectAll();
}
