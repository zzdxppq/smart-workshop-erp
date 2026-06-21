package com.btsheng.erp.business.crm.warehousescan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseScan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmWarehouseScanMapper extends BaseMapper<CrmWarehouseScan> {

    @Select("SELECT * FROM crm_warehouse_scan WHERE scan_no = #{scanNo} LIMIT 1")
    CrmWarehouseScan selectByScanNo(@Param("scanNo") String scanNo);

    @Select("SELECT * FROM crm_warehouse_scan WHERE client_id = #{clientId} AND sync_status = 'PENDING' ORDER BY scanned_at")
    List<CrmWarehouseScan> selectPendingByClient(@Param("clientId") String clientId);

    @Select("SELECT * FROM crm_warehouse_scan WHERE barcode_no = #{barcodeNo} ORDER BY scanned_at DESC LIMIT #{limit}")
    List<CrmWarehouseScan> selectByBarcodeNo(@Param("barcodeNo") String barcodeNo, @Param("limit") int limit);

    @Select("SELECT id, scan_no AS scanNo, scan_type AS scanType, barcode_no AS barcodeNo, " +
            "material_code AS materialCode, location_code AS locationCode, qty, workorder_no AS workorderNo, " +
            "sync_status AS syncStatus, scanned_by AS scannedBy, scanned_at AS scannedAt " +
            "FROM crm_warehouse_scan " +
            "WHERE (#{scanType} IS NULL OR scan_type = #{scanType}) " +
            "AND (#{syncStatus} IS NULL OR sync_status = #{syncStatus}) " +
            "AND (#{barcodeNo} IS NULL OR barcode_no = #{barcodeNo}) " +
            "ORDER BY scanned_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectScans(@Param("scanType") String scanType,
                                           @Param("syncStatus") String syncStatus,
                                           @Param("barcodeNo") String barcodeNo,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);
}
