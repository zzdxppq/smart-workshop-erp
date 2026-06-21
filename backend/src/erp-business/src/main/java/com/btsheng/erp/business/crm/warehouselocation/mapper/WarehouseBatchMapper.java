package com.btsheng.erp.business.crm.warehouselocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WarehouseBatchMapper extends BaseMapper<CrmBatch> {

    String BATCH_SELECT = """
            SELECT b.id, b.batch_no, b.material_id, b.po_id, b.po_item_id, b.quantity,
                   b.arrived_at, b.quality_status, b.created_by, b.created_at,
                   m.material_code,
                   po.supplier_id, po.supplier_name
            FROM crm_batch b
            JOIN crm_material m ON b.material_id = m.id
            LEFT JOIN crm_purchase_order po ON b.po_id = po.id AND b.po_id > 0
            """;

    @Select(BATCH_SELECT + " WHERE b.batch_no = #{batchNo} LIMIT 1")
    CrmBatch selectByBatchNo(@Param("batchNo") String batchNo);

    @Select(BATCH_SELECT + " WHERE m.material_code = #{materialCode} ORDER BY b.arrived_at ASC")
    List<CrmBatch> selectByMaterialFefo(@Param("materialCode") String materialCode);

    @Select(BATCH_SELECT
            + " WHERE m.material_code = #{materialCode} AND b.quality_status = #{qualityStatus}"
            + " ORDER BY b.arrived_at ASC")
    List<CrmBatch> selectByMaterialAndQuality(@Param("materialCode") String materialCode,
                                               @Param("qualityStatus") String qualityStatus);

    @Select(BATCH_SELECT + " WHERE po.supplier_id = #{supplierId} ORDER BY b.arrived_at DESC")
    List<CrmBatch> selectBySupplier(@Param("supplierId") Long supplierId);

    @Select("""
            SELECT po.po_no AS sourceNo, po.po_no AS materialCode,
                   po.id AS id, po.id AS materialId,
                   1 AS expectedQty, po.status AS status,
                   po.created_at AS createdAt,
                   '采购' AS sourceType
            FROM crm_purchase_order po
            WHERE po.approval_status = 'APPROVED'
              AND po.status <> 'CANCELLED'
            ORDER BY po.created_at DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectInboundPending(@Param("limit") int limit);
}
