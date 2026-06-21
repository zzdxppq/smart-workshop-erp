package com.btsheng.erp.business.crm.materialbarcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialBarcode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.11 · 物料条码 Mapper
 */
@Mapper
public interface CrmMaterialBarcodeMapper extends BaseMapper<CrmMaterialBarcode> {

    @Select("SELECT * FROM crm_material_barcode WHERE barcode_no = #{barcodeNo} LIMIT 1")
    CrmMaterialBarcode selectByBarcodeNo(@Param("barcodeNo") String barcodeNo);

    @Select("SELECT * FROM crm_material_barcode WHERE material_code = #{materialCode} ORDER BY generated_at DESC")
    List<CrmMaterialBarcode> selectByMaterialCode(@Param("materialCode") String materialCode);

    @Select("SELECT b.id, b.barcode_no AS barcodeNo, b.material_code AS materialCode, b.spec, " +
            "b.process_id AS processId, b.batch_no AS batchNo, b.qty, b.status, " +
            "b.generated_by AS generatedBy, b.generated_at AS generatedAt, " +
            "m.id AS materialId " +
            "FROM crm_material_barcode b " +
            "LEFT JOIN crm_material m ON m.material_code = b.material_code " +
            "WHERE (#{materialCode} IS NULL OR b.material_code = #{materialCode}) " +
            "AND (#{status} IS NULL OR b.status = #{status}) " +
            "AND (#{keyword} IS NULL OR b.barcode_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR b.material_code LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY b.generated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectBarcodes(@Param("keyword") String keyword,
                                              @Param("materialCode") String materialCode,
                                              @Param("status") String status,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM crm_material_barcode " +
            "WHERE (#{materialCode} IS NULL OR material_code = #{materialCode}) " +
            "AND (#{status} IS NULL OR status = #{status})")
    long countBarcodes(@Param("materialCode") String materialCode,
                        @Param("status") String status);

    @Select("SELECT * FROM crm_material_barcode WHERE material_code = #{materialCode} AND status = 'ACTIVE' LIMIT 1")
    CrmMaterialBarcode selectActiveByMaterialCode(@Param("materialCode") String materialCode);
}
