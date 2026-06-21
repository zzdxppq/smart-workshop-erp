package com.btsheng.erp.business.crm.inventoryalert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventoryAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmInventoryAlertMapper extends BaseMapper<CrmInventoryAlert> {

    @Select("SELECT * FROM crm_inventory_alert WHERE material_code = #{code} AND status = 'OPEN' LIMIT 1")
    CrmInventoryAlert selectOpenByMaterial(@Param("code") String code);

    @Select("SELECT alert_level, COUNT(*) AS cnt FROM crm_inventory_alert " +
            "WHERE status = 'OPEN' GROUP BY alert_level")
    List<Map<String, Object>> aggregateByLevel();

    @Select("SELECT id, material_code AS materialCode, alert_level AS alertLevel, " +
            "current_qty AS currentQty, min_qty AS minQty, message, status, " +
            "triggered_at AS triggeredAt, resolved_at AS resolvedAt " +
            "FROM crm_inventory_alert " +
            "WHERE (#{status} IS NULL OR status = #{status}) " +
            "AND (#{level} IS NULL OR alert_level = #{level}) " +
            "ORDER BY triggered_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectAlerts(@Param("status") String status,
                                            @Param("level") String level,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
}
