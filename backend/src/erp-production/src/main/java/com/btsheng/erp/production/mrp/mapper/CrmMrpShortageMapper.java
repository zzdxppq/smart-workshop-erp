package com.btsheng.erp.production.mrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.mrp.entity.CrmMrpShortage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmMrpShortageMapper extends BaseMapper<CrmMrpShortage> {
    @Select("SELECT * FROM crm_mrp_shortage WHERE run_id = #{runId} ORDER BY priority ASC, shortage_qty DESC")
    List<CrmMrpShortage> selectByRunId(@Param("runId") Long runId);

    @Select("SELECT * FROM crm_mrp_shortage WHERE material_code = #{code} ORDER BY priority ASC")
    List<CrmMrpShortage> selectByMaterial(@Param("code") String materialCode);
}
