package com.btsheng.erp.business.crm.purchaseinspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingInspection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmPurchaseIncomingInspectionMapper extends BaseMapper<CrmPurchaseIncomingInspection> {

    @Select("SELECT * FROM crm_purchase_incoming_inspection WHERE po_id = #{poId} AND material_id = #{materialId}")
    CrmPurchaseIncomingInspection selectByPoAndMaterial(@Param("poId") Long poId, @Param("materialId") Long materialId);

    @Select("SELECT * FROM crm_purchase_incoming_inspection WHERE result = #{result} ORDER BY inspected_at DESC")
    List<CrmPurchaseIncomingInspection> selectByResult(@Param("result") String result);

    @Select("SELECT * FROM crm_purchase_incoming_inspection ORDER BY created_at DESC")
    List<CrmPurchaseIncomingInspection> selectAll();
}
