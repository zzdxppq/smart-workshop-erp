package com.btsheng.erp.production.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.material.entity.CrmMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrmMaterialMapper extends BaseMapper<CrmMaterial> {

    @Select("SELECT * FROM crm_material WHERE id = #{id} AND is_active = 1 LIMIT 1")
    CrmMaterial selectActiveById(Long id);

    @Select("SELECT * FROM crm_material WHERE material_code = #{materialCode} AND is_active = 1 LIMIT 1")
    CrmMaterial selectByCode(String materialCode);
}
