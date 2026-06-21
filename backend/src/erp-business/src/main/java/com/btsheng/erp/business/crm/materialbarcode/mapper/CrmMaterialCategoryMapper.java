package com.btsheng.erp.business.crm.materialbarcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmMaterialCategoryMapper extends BaseMapper<CrmMaterialCategory> {

    @Select("SELECT * FROM crm_material_category WHERE category_code = #{categoryCode} LIMIT 1")
    CrmMaterialCategory selectByCategoryCode(@Param("categoryCode") String categoryCode);

    @Select("SELECT * FROM crm_material_category WHERE is_active = 1 ORDER BY seq_no")
    List<CrmMaterialCategory> selectAllActive();

    @Select("SELECT * FROM crm_material_category WHERE prefix = #{prefix} LIMIT 1")
    CrmMaterialCategory selectByPrefix(@Param("prefix") String prefix);
}
