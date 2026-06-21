package com.btsheng.erp.business.crm.drawing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * V1.3.9 Sprint 12 Story 12.1 + Sprint 13 Story 13.3 · 图纸-业务单据关联 Mapper
 *
 * <p>12.1 关键查询（5）：
 * <ul>
 *   <li>{@link #existsByDrawingAndBizTypeAndBizIdIn} 鉴权核心（drawing → 当前用户 biz_ids 命中）</li>
 *   <li>{@link #selectByDrawingId} 图纸 → 业务单据（DrawingAuthz 关联过滤）</li>
 *   <li>{@link #selectByBiz} 业务单据 → 图纸（5 类业务单据详情页"查看关联图纸"）</li>
 *   <li>{@link #selectBizIdsByDrawingAndBizType} OPERATOR 工序关联查找</li>
 *   <li>{@link #selectDrawingIdsByBizRef} 反向查询（供 web 端使用）</li>
 * </ul>
 *
 * <p>13.3 新增 5 类 link JOIN 真实查询（复用 V54 crm_drawing_link · material_code 路径）：
 * <ul>
 *   <li>{@link #selectOrderBizIdsByDrawing} 图纸 → ORDER bizIds（JOIN crm_order_item + crm_order）</li>
 *   <li>{@link #selectPoBizIdsByDrawing} 图纸 → PO bizIds（JOIN crm_purchase_order_item + purchase_order）</li>
 *   <li>{@link #selectIncomingBizIdsByDrawing} 图纸 → INCOMING bizIds（JOIN crm_incoming_order_item + wms_inbound）</li>
 *   <li>{@link #selectInspectionBizIdsByDrawing} 图纸 → INSPECTION bizIds（JOIN crm_inspection_item + qc_inspection）</li>
 *   <li>{@link #selectDrawingsByBizRefAccessible} 业务单据 → 可访问图纸列表（端点 2）</li>
 *   <li>{@link #selectOperatorProcessDrawings} 工序 → 图纸列表（端点 3 · OPERATOR 扫码）</li>
 * </ul>
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14 (12.1) · 2026-06-14 (13.3 扩展)
 */
@Mapper
public interface CrmDrawingLinkMapper extends BaseMapper<CrmDrawingLink> {

    // ============================================================
    // 12.1 既有 5 查询
    // ============================================================

    /**
     * 鉴权核心：图纸 + bizType + 用户持有的 bizIds 列表 → 至少一条命中即 true。
     */
    @Select("SELECT COUNT(1) FROM crm_drawing_link " +
            "WHERE drawing_id = #{drawingId} AND biz_type = #{bizType} " +
            "AND biz_id IN " +
            "<foreach collection='bizIds' item='bid' open='(' separator=',' close=')'>" +
            "  #{bid}" +
            "</foreach>")
    long existsByDrawingAndBizTypeAndBizIdIn(@Param("drawingId") Long drawingId,
                                              @Param("bizType") String bizType,
                                              @Param("bizIds") List<Long> bizIds);

    /** 图纸 → 全部业务关联（用于 permission 端点返回 linkedBizIds） */
    @Select("SELECT * FROM crm_drawing_link WHERE drawing_id = #{drawingId}")
    List<CrmDrawingLink> selectByDrawingId(@Param("drawingId") Long drawingId);

    /** 业务单据 → 关联图纸（详情页"查看关联图纸"入口） */
    @Select("SELECT * FROM crm_drawing_link WHERE biz_type = #{bizType} AND biz_id = #{bizId}")
    List<CrmDrawingLink> selectByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    /** 图纸 → 指定 bizType 下的 bizId 列表（用于 OPERATOR 关联判断） */
    @Select("SELECT biz_id FROM crm_drawing_link WHERE drawing_id = #{drawingId} AND biz_type = #{bizType}")
    List<Long> selectBizIdsByDrawingAndBizType(@Param("drawingId") Long drawingId,
                                                @Param("bizType") String bizType);

    /** 反向查询：业务单据 → 关联 drawing_id 列表 */
    @Select("SELECT drawing_id FROM crm_drawing_link WHERE biz_type = #{bizType} AND biz_id = #{bizId}")
    List<Long> selectDrawingIdsByBizRef(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    // ============================================================
    // 13.3 真实查询 · 5 类 link JOIN · 端点 1
    // ============================================================

    /**
     * 13.3 端点 1（ORDER）· 图纸 → 订单 bizIds
     *
     * <p>JOIN 路径：crm_drawing → crm_drawing_link (biz_type='ORDER') → crm_order_item (material) → crm_order
     * <p>权限过滤：crm_order.owner_user_id = :userId AND status NOT IN ('DRAFT','CANCELLED')
     * <p>索引：idx_drawing_link_order (CASE WHEN biz_type='ORDER' THEN drawing_id/biz_id) + idx_order_item_material_order
     */
    @Select("SELECT DISTINCT o.id AS biz_id " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'ORDER' " +
            "JOIN crm_order_item oi ON oi.material = d.material_code " +
            "JOIN crm_order o ON o.id = oi.order_id AND o.owner_user_id = #{userId} " +
            "WHERE d.id = #{drawingId} " +
            "  AND o.status NOT IN ('DRAFT','CANCELLED') " +
            "  AND o.is_deleted = 0")
    List<Long> selectOrderBizIdsByDrawing(@Param("drawingId") Long drawingId,
                                          @Param("userId") Long userId);

    /**
     * 13.3 端点 1（PO）· 图纸 → PO bizIds
     *
     * <p>JOIN 路径：crm_drawing → crm_drawing_link (biz_type='PO') → crm_purchase_order_item → purchase_order
     * <p>权限过滤：purchase_order.created_by = :userId AND status NOT IN ('DRAFT','CANCELLED')
     */
    @Select("SELECT DISTINCT po.id AS biz_id " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'PO' " +
            "JOIN crm_purchase_order_item poi ON poi.material_code = d.material_code " +
            "JOIN crm_purchase_order po ON po.id = poi.purchase_order_id AND po.created_by = #{userId} " +
            "WHERE d.id = #{drawingId} " +
            "  AND po.status NOT IN ('CANCELLED')")
    List<Long> selectPoBizIdsByDrawing(@Param("drawingId") Long drawingId,
                                        @Param("userId") Long userId);

    /**
     * 13.3 端点 1（INCOMING）· 图纸 → 入库单 bizIds
     *
     * <p>JOIN 路径：crm_drawing → crm_drawing_link (biz_type='INCOMING') → crm_incoming_order_item → wms_inbound
     * <p>权限过滤：wms_inbound.operator_user_id = :userId AND status NOT IN ('DRAFT','CANCELLED')
     */
    @Select("SELECT DISTINCT wi.id AS biz_id " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'INCOMING' " +
            "JOIN crm_incoming_order_item ioi ON ioi.material_code = d.material_code " +
            "JOIN wms_inbound wi ON wi.id = ioi.incoming_order_id AND wi.operator_user_id = #{userId} " +
            "WHERE d.id = #{drawingId} " +
            "  AND wi.status NOT IN ('DRAFT','CANCELLED')")
    List<Long> selectIncomingBizIdsByDrawing(@Param("drawingId") Long drawingId,
                                              @Param("userId") Long userId);

    /**
     * 13.3 端点 1（INSPECTION）· 图纸 → 质检单 bizIds
     *
     * <p>JOIN 路径：crm_drawing → crm_drawing_link (biz_type='INSPECTION') → crm_inspection_item → qc_inspection
     * <p>权限过滤：qc_inspection.inspector_user_id = :userId
     */
    @Select("SELECT DISTINCT qi.id AS biz_id " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'INSPECTION' " +
            "JOIN crm_inspection_item ii ON ii.material_code = d.material_code " +
            "JOIN qc_inspection qi ON qi.id = ii.inspection_id AND qi.inspector_user_id = #{userId} " +
            "WHERE d.id = #{drawingId}")
    List<Long> selectInspectionBizIdsByDrawing(@Param("drawingId") Long drawingId,
                                                @Param("userId") Long userId);

    // ============================================================
    // 13.3 真实查询 · 端点 2 · 业务单据 → 可访问图纸列表（5 类分方法）
    // ============================================================

    /**
     * 13.3 端点 2（ORDER）· 业务单据 → 可访问图纸列表
     */
    @Select("SELECT DISTINCT d.id AS drawing_id, d.drawing_no AS drawing_code, d.title AS drawing_name, " +
            "       d.version, d.pdf_path AS thumbnail_url " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'ORDER' " +
            "JOIN crm_order_item oi ON oi.material = d.material_code " +
            "WHERE oi.order_id = #{bizId}")
    List<Map<String, Object>> selectDrawingsByOrderBizRef(@Param("bizId") Long bizId);

    /**
     * 13.3 端点 2（PO）· 业务单据 → 可访问图纸列表
     */
    @Select("SELECT DISTINCT d.id AS drawing_id, d.drawing_no AS drawing_code, d.title AS drawing_name, " +
            "       d.version, d.pdf_path AS thumbnail_url " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'PO' " +
            "JOIN crm_purchase_order_item poi ON poi.material_code = d.material_code " +
            "WHERE poi.purchase_order_id = #{bizId}")
    List<Map<String, Object>> selectDrawingsByPoBizRef(@Param("bizId") Long bizId);

    /**
     * 13.3 端点 2（INCOMING）· 业务单据 → 可访问图纸列表
     */
    @Select("SELECT DISTINCT d.id AS drawing_id, d.drawing_no AS drawing_code, d.title AS drawing_name, " +
            "       d.version, d.pdf_path AS thumbnail_url " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'INCOMING' " +
            "JOIN crm_incoming_order_item ioi ON ioi.material_code = d.material_code " +
            "WHERE ioi.incoming_order_id = #{bizId}")
    List<Map<String, Object>> selectDrawingsByIncomingBizRef(@Param("bizId") Long bizId);

    /**
     * 13.3 端点 2（INSPECTION）· 业务单据 → 可访问图纸列表
     */
    @Select("SELECT DISTINCT d.id AS drawing_id, d.drawing_no AS drawing_code, d.title AS drawing_name, " +
            "       d.version, d.pdf_path AS thumbnail_url " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'INSPECTION' " +
            "JOIN crm_inspection_item ii ON ii.material_code = d.material_code " +
            "WHERE ii.inspection_id = #{bizId}")
    List<Map<String, Object>> selectDrawingsByInspectionBizRef(@Param("bizId") Long bizId);

    /**
     * 13.3 端点 2（WORKORDER_PROCESS）· 业务单据 → 可访问图纸列表
     */
    @Select("SELECT DISTINCT d.id AS drawing_id, d.drawing_no AS drawing_code, d.title AS drawing_name, " +
            "       d.version, d.pdf_path AS thumbnail_url " +
            "FROM crm_drawing d " +
            "JOIN crm_drawing_link dl ON dl.drawing_id = d.id AND dl.biz_type = 'WORKORDER_PROCESS' " +
            "JOIN crm_workorder_process wp ON wp.material_code = d.material_code " +
            "WHERE wp.id = #{bizId}")
    List<Map<String, Object>> selectDrawingsByWorkorderProcessBizRef(@Param("bizId") Long bizId);

    // ============================================================
    // 13.3 真实查询 · 端点 3 · OPERATOR 工序扫码 → 图纸列表
    // ============================================================

    /**
     * 13.3 端点 3 · 工序 → 图纸列表（含工序/工单信息 · IN_PROGRESS 过滤）
     *
     * <p>JOIN 路径：crm_workorder_process → crm_workorder → crm_workorder_item → crm_drawing
     * <p>过滤：wp.status = 'IN_PROGRESS'（端点 3 鉴权层强制 operator_user_id 匹配）
     */
    @Select("SELECT DISTINCT d.id AS drawing_id, d.drawing_no AS drawing_code, d.title AS drawing_name, " +
            "       d.version, d.pdf_path AS thumbnail_url, " +
            "       wp.id AS process_id, wp.process_code, wp.process_name, " +
            "       wp.work_order_id, wo.workorder_no AS work_order_code, " +
            "       wp.status, wp.operator_user_id " +
            "FROM crm_workorder_process wp " +
            "JOIN crm_workorder wo ON wo.id = wp.work_order_id " +
            "JOIN crm_workorder_item woi ON woi.work_order_id = wo.id " +
            "JOIN crm_drawing d ON d.material_code = woi.material_code " +
            "WHERE wp.id = #{processId} " +
            "  AND wp.status = 'IN_PROGRESS'")
    List<Map<String, Object>> selectOperatorProcessDrawings(@Param("processId") Long processId);

    // ============================================================
    // 13.3 鉴权辅助 · 按用户查 bizIds（不依赖 drawingId）
    // ============================================================
            @Select("SELECT DISTINCT o.id FROM crm_order o " +
            "WHERE o.owner_user_id = #{userId} AND o.status NOT IN ('DRAFT','CANCELLED') AND o.is_deleted = 0")
    List<Long> selectOrderIdsByUser(@Param("userId") Long userId);

    @Select("SELECT DISTINCT po.id FROM crm_purchase_order po " +
            "WHERE po.created_by = #{userId} AND po.status NOT IN ('CANCELLED')")
    List<Long> selectPoIdsByUser(@Param("userId") Long userId);

    @Select("SELECT DISTINCT wi.id FROM wms_inbound wi " +
            "WHERE wi.operator_user_id = #{userId} AND wi.status NOT IN ('DRAFT','CANCELLED')")
    List<Long> selectIncomingIdsByUser(@Param("userId") Long userId);

    @Select("SELECT DISTINCT qi.id FROM qc_inspection qi WHERE qi.inspector_user_id = #{userId}")
    List<Long> selectInspectionIdsByUser(@Param("userId") Long userId);

    @Select("SELECT wp.id FROM crm_workorder_process wp " +
            "WHERE wp.operator_user_id = #{userId} AND wp.status = 'IN_PROGRESS'")
    List<Long> selectOperatorProcessIdsByUser(@Param("userId") Long userId);
}