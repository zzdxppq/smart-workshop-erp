package com.btsheng.erp.business.crm.materialbarcodebatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.materialbarcodebatch.entity.CrmMaterialBarcodeBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * V1.3.8 · Story 3.2 · crm_material_barcode_batch Mapper
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Mapper
public interface CrmMaterialBarcodeBatchMapper extends BaseMapper<CrmMaterialBarcodeBatch> {

    /**
     * 按 barcode_no 查（扫码解析用）
     */
    @Select("""
            SELECT * FROM crm_material_barcode_batch
            WHERE barcode_no = #{barcodeNo} AND is_active = 1
            """)
    CrmMaterialBarcodeBatch selectByBarcodeNo(@Param("barcodeNo") String barcodeNo);
}