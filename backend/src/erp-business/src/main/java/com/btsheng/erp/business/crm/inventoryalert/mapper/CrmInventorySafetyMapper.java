package com.btsheng.erp.business.crm.inventoryalert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventorySafety;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmInventorySafetyMapper extends BaseMapper<CrmInventorySafety> {
    @Select("SELECT * FROM crm_inventory_safety WHERE material_code = #{code} LIMIT 1")
    CrmInventorySafety selectByMaterialCode(@Param("code") String code);

    @Select("SELECT * FROM crm_inventory_safety WHERE enabled = 1 ORDER BY material_code")
    List<CrmInventorySafety> selectAllEnabled();
}
