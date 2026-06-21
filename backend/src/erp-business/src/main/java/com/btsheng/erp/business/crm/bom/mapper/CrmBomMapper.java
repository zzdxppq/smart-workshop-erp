package com.btsheng.erp.business.crm.bom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.9 · AC-3.3 BOM Mapper
 */
@Mapper
public interface CrmBomMapper extends BaseMapper<CrmBom> {

    @Select("SELECT * FROM crm_bom WHERE bom_no = #{bomNo} AND bom_version = #{bomVersion} LIMIT 1")
    CrmBom selectByBomNoAndVersion(String bomNo, String bomVersion);

    @Select("SELECT * FROM crm_bom WHERE bom_no = #{bomNo} ORDER BY id DESC LIMIT 1")
    CrmBom selectByBomNo(String bomNo);

    @Select("SELECT * FROM crm_bom WHERE material_code = #{materialCode} AND bom_version = #{bomVersion} LIMIT 1")
    CrmBom selectByMaterialCodeAndVersion(String materialCode, String bomVersion);

    @Select("SELECT * FROM crm_bom WHERE drawing_id = #{drawingId} AND bom_version = #{bomVersion} LIMIT 1")
    CrmBom selectByDrawingIdAndVersion(Long drawingId, String bomVersion);

    @Select("SELECT * FROM crm_bom WHERE parent_bom_id = #{parentBomId} ORDER BY bom_level")
    List<CrmBom> selectByParentId(Long parentBomId);

    @Select("SELECT status, COUNT(*) AS cnt FROM crm_bom GROUP BY status")
    List<Map<String, Object>> countByStatus();
}
