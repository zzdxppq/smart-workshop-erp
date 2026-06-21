package com.btsheng.erp.business.crm.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.batch.entity.CrmBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 · Story 3.1 · crm_batch Mapper
 *
 * <p>聚合查询：按 po_id 统计物料到货进度（用于 PO 状态机决策）。
 * 风格：与 V1.3.7 CrmIncomingMapper 一致，使用 @Select 注解而非 XML。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Mapper
public interface CrmBatchMapper extends BaseMapper<CrmBatch> {

    /**
     * 按 po_id 聚合物料到货进度
     * <p>返回：[{material_id, ordered, arrived, batch_count, quality_status}]
     * <p>JOIN 逻辑：crm_batch LEFT JOIN crm_purchase_order_item 取 ordered 数量
     */
    @Select("""
            SELECT
              poi.material_id        AS material_id,
              poi.quantity           AS ordered,
              COALESCE(SUM(b.quantity), 0) AS arrived,
              COUNT(b.id)            AS batch_count,
              MAX(b.quality_status)  AS quality_status
            FROM crm_purchase_order_item poi
            LEFT JOIN crm_batch b
              ON b.po_id = #{poId} AND b.material_id = poi.material_id
            WHERE poi.po_id = #{poId}
            GROUP BY poi.material_id, poi.quantity
            """)
    List<Map<String, Object>> aggregatePoProgress(@Param("poId") Long poId);

    /**
     * 双写对比：crm_batch vs crm_batch_shadow（按 batch_no + material_id 分组）
     * <p>返回：{total, matched, mismatched}
     */
    @Select("""
            SELECT
              COUNT(*) AS total,
              SUM(CASE WHEN a.cnt = b.cnt THEN 1 ELSE 0 END) AS matched
            FROM (
              SELECT batch_no, material_id, COUNT(*) AS cnt, SUM(quantity) AS qty
              FROM crm_batch
              WHERE created_at >= #{sinceTime}
              GROUP BY batch_no, material_id
            ) a
            JOIN (
              SELECT batch_no, material_id, COUNT(*) AS cnt, SUM(quantity) AS qty
              FROM crm_batch_shadow
              WHERE created_at >= #{sinceTime}
              GROUP BY batch_no, material_id
            ) b
              ON a.batch_no = b.batch_no AND a.material_id = b.material_id
            """)
    Map<String, Object> compareShadow(@Param("sinceTime") LocalDateTime sinceTime);
}