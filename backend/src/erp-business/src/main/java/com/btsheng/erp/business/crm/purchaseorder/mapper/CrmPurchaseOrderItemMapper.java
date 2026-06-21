package com.btsheng.erp.business.crm.purchaseorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrmPurchaseOrderItemMapper extends BaseMapper<CrmPurchaseOrderItem> {

    @Select("SELECT d.id FROM crm_drawing d WHERE d.material_code = #{materialCode} AND d.status IN ('DRAFT','RELEASED') ORDER BY d.id LIMIT 1")
    Long selectDrawingIdByMaterialCode(@Param("materialCode") String materialCode);
}
