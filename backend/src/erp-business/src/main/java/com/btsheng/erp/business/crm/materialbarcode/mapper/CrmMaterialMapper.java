package com.btsheng.erp.business.crm.materialbarcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.11 · 物料主数据 Mapper
 * V2.1 改造：新增 selectByDrawingNo 方法
 */
@Mapper
public interface CrmMaterialMapper extends BaseMapper<CrmMaterial> {

    @Select("SELECT * FROM crm_material WHERE id = #{id} AND is_active = 1 LIMIT 1")
    CrmMaterial selectActiveById(@Param("id") Long id);

    @Select("SELECT * FROM crm_material WHERE material_code = #{materialCode} LIMIT 1")
    CrmMaterial selectByMaterialCode(@Param("materialCode") String materialCode);

    @Select("SELECT * FROM crm_material WHERE category_id = #{categoryId} AND is_active = 1 ORDER BY material_code")
    List<CrmMaterial> selectByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM crm_material WHERE is_active = 1 ORDER BY material_code")
    List<CrmMaterial> selectAllActive();

    @Select("SELECT * FROM crm_material WHERE is_active = 1 " +
            "AND material_code LIKE CONCAT(#{prefix}, '%') ORDER BY material_code LIMIT #{limit}")
    List<CrmMaterial> selectByCodePrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    /**
     * V2.1 根据图号查询物料
     */
    @Select("SELECT * FROM crm_material WHERE drawing_no = #{drawingNo} AND is_active = 1 LIMIT 1")
    CrmMaterial selectByDrawingNo(@Param("drawingNo") String drawingNo);
}
