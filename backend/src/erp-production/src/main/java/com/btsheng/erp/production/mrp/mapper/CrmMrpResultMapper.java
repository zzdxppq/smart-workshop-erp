package com.btsheng.erp.production.mrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.mrp.entity.CrmMrpResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmMrpResultMapper extends BaseMapper<CrmMrpResult> {
    @Select("SELECT * FROM crm_mrp_result WHERE run_id = #{runId} ORDER BY shortage_qty DESC")
    List<CrmMrpResult> selectByRunId(@Param("runId") Long runId);

    @Select("SELECT * FROM crm_mrp_result WHERE material_code = #{code} ORDER BY id DESC LIMIT 1")
    CrmMrpResult selectLatestByMaterial(@Param("code") String materialCode);
}
