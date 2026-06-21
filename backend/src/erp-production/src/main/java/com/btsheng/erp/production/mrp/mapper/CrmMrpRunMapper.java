package com.btsheng.erp.production.mrp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.mrp.entity.CrmMrpRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmMrpRunMapper extends BaseMapper<CrmMrpRun> {

    @Select("SELECT * FROM crm_mrp_run WHERE run_no = #{runNo} LIMIT 1")
    CrmMrpRun selectByRunNo(@Param("runNo") String runNo);

    @Select("SELECT id, run_no AS runNo, run_type AS runType, status, started_at AS startedAt, " +
            "completed_at AS completedAt, total_shortage AS totalShortage, " +
            "total_purchase_suggestion AS totalPurchaseSuggestion, triggered_by AS triggeredBy, " +
            "remark AS remark " +
            "FROM crm_mrp_run " +
            "WHERE (#{status} IS NULL OR status = #{status}) " +
            "ORDER BY started_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectRuns(@Param("status") String status,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);
}
