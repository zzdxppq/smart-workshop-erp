package com.btsheng.erp.production.outsource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmOutsourceOrderMapper extends BaseMapper<CrmOutsourceOrder> {

    @Select("SELECT * FROM crm_outsource_order WHERE outsource_no = #{outsourceNo} LIMIT 1")
    CrmOutsourceOrder selectByOutsourceNo(@Param("outsourceNo") String outsourceNo);

    @Select("SELECT id, outsource_no AS outsourceNo, workorder_no AS workorderNo, step_no AS stepNo, " +
            "supplier_id AS supplierId, supplier_name AS supplierName, process_name AS processName, " +
            "material_code AS materialCode, qty, unit_price AS unitPrice, total_amount AS totalAmount, " +
            "delivery_date AS deliveryDate, status, rework_count AS reworkCount, is_urgent AS isUrgent, " +
            "creator_user_id AS creatorUserId, created_at AS createdAt " +
            "FROM crm_outsource_order " +
            "WHERE (#{status} IS NULL OR status = #{status}) " +
            "AND (#{workorderNo} IS NULL OR workorder_no = #{workorderNo}) " +
            "AND (#{supplierId} IS NULL OR supplier_id = #{supplierId}) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectOutsourceOrders(@Param("status") String status,
                                                     @Param("workorderNo") String workorderNo,
                                                     @Param("supplierId") Long supplierId,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);

    @Select("SELECT unit_price FROM crm_outsource_order "
            + "WHERE supplier_id = #{supplierId} AND process_name = #{processName} "
            + "AND unit_price IS NOT NULL AND status NOT IN ('DRAFT','CLOSED') "
            + "ORDER BY created_at DESC LIMIT 3")
    List<java.math.BigDecimal> selectRecentPrices(@Param("supplierId") Long supplierId,
                                                  @Param("processName") String processName);

    @Select("SELECT unit_price FROM crm_outsource_order "
            + "WHERE supplier_id = #{supplierId} AND material_code = #{materialCode} "
            + "AND unit_price IS NOT NULL AND status NOT IN ('DRAFT','CLOSED') "
            + "ORDER BY created_at DESC LIMIT 3")
    List<java.math.BigDecimal> selectRecentPricesByMaterial(@Param("supplierId") Long supplierId,
                                                            @Param("materialCode") String materialCode);
}
