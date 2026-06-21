package com.btsheng.erp.production.scan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.scan.entity.CrmProductionScan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmProductionScanMapper extends BaseMapper<CrmProductionScan> {
    @Select("SELECT * FROM crm_production_scan WHERE workorder_no = #{wo} ORDER BY scanned_at DESC LIMIT #{limit}")
    List<CrmProductionScan> selectByWorkorder(@Param("wo") String workorderNo, @Param("limit") int limit);

    @Select("SELECT * FROM crm_production_scan WHERE operator_user_id = #{userId} " +
            "AND scan_type = #{scanType} ORDER BY scanned_at DESC LIMIT #{limit}")
    List<CrmProductionScan> selectByOperatorAndType(@Param("userId") Long userId,
                                                     @Param("scanType") String scanType,
                                                     @Param("limit") int limit);

    @Select("SELECT id, scan_no AS scanNo, workorder_no AS workorderNo, scan_type AS scanType, " +
            "operator_user_id AS operatorUserId, qty, step_no AS stepNo, scanned_at AS scannedAt " +
            "FROM crm_production_scan " +
            "WHERE (#{workorderNo} IS NULL OR workorder_no = #{workorderNo}) " +
            "AND (#{scanType} IS NULL OR scan_type = #{scanType}) " +
            "ORDER BY scanned_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectScans(@Param("workorderNo") String workorderNo,
                                           @Param("scanType") String scanType,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);
}
