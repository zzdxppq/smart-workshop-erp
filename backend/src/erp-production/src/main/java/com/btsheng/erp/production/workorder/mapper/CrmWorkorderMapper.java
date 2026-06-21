package com.btsheng.erp.production.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmWorkorderMapper extends BaseMapper<CrmWorkorder> {

    @Select("SELECT * FROM crm_workorder WHERE workorder_no = #{no} LIMIT 1")
    CrmWorkorder selectByNo(@Param("no") String no);

    @Select("SELECT * FROM crm_workorder WHERE sales_order_id = #{orderId} LIMIT 1")
    CrmWorkorder selectBySalesOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM crm_workorder WHERE material_code = #{materialCode} AND sales_order_id IS NULL ORDER BY created_at DESC LIMIT 1")
    CrmWorkorder selectUnlinkedByMaterial(@Param("materialCode") String materialCode);

    @Select("SELECT COUNT(*) FROM crm_workorder " +
            "WHERE (#{status} IS NULL OR status = #{status}) " +
            "AND (#{materialCode} IS NULL OR material_code = #{materialCode}) " +
            "AND (#{scopeOwnerId} IS NULL OR owner_user_id = #{scopeOwnerId}) " +
            "AND (#{scopeDeptId} IS NULL OR dept_id = #{scopeDeptId}) " +
            "AND (#{keyword} IS NULL OR workorder_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR product_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR material_code LIKE CONCAT('%', #{keyword}, '%'))")
    long countWorkorders(@Param("keyword") String keyword,
                         @Param("status") String status,
                         @Param("materialCode") String materialCode,
                         @Param("scopeOwnerId") Long scopeOwnerId,
                         @Param("scopeDeptId") Long scopeDeptId);

    @Select("SELECT id, workorder_no AS workorderNo, material_code AS materialCode, " +
            "product_name AS productName, qty, unit, priority, status, " +
            "equipment_id AS equipmentId, equipment_type AS equipmentType, " +
            "scheduled_start AS scheduledStart, scheduled_end AS scheduledEnd, " +
            "estimated_hours AS estimatedHours, is_fa AS isFa, " +
            "sales_order_id AS salesOrderId, sales_order_no AS salesOrderNo, " +
            "created_by AS createdBy, created_at AS createdAt " +
            "FROM crm_workorder " +
            "WHERE (#{status} IS NULL OR status = #{status}) " +
            "AND (#{materialCode} IS NULL OR material_code = #{materialCode}) " +
            "AND (#{scopeOwnerId} IS NULL OR owner_user_id = #{scopeOwnerId}) " +
            "AND (#{scopeDeptId} IS NULL OR dept_id = #{scopeDeptId}) " +
            "AND (#{keyword} IS NULL OR workorder_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR product_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR material_code LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY priority ASC, created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectWorkorders(@Param("keyword") String keyword,
                                                @Param("status") String status,
                                                @Param("materialCode") String materialCode,
                                                @Param("scopeOwnerId") Long scopeOwnerId,
                                                @Param("scopeDeptId") Long scopeDeptId,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);
}
