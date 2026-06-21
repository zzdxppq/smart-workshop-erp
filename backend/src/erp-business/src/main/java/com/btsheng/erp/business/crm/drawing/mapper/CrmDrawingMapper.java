package com.btsheng.erp.business.crm.drawing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.7 · 图纸 Mapper
 */
@Mapper
public interface CrmDrawingMapper extends BaseMapper<CrmDrawing> {

    @Select("SELECT * FROM crm_drawing WHERE drawing_no = #{drawingNo} AND version = #{version} LIMIT 1")
    CrmDrawing selectByDrawingNoAndVersion(@Param("drawingNo") String drawingNo, @Param("version") String version);

    @Select("SELECT * FROM crm_drawing WHERE drawing_no = #{drawingNo} ORDER BY created_at DESC LIMIT 1")
    CrmDrawing selectLatestByDrawingNo(@Param("drawingNo") String drawingNo);

    @Select("SELECT * FROM crm_drawing WHERE material_code = #{materialCode} LIMIT 1")
    CrmDrawing selectByMaterialCode(@Param("materialCode") String materialCode);

    @Select("SELECT version FROM crm_drawing_version WHERE drawing_id = #{drawingId} ORDER BY changed_at DESC")
    List<String> selectVersionsByDrawingId(@Param("drawingId") Long drawingId);

    @Select("SELECT MAX(version) FROM crm_drawing_version WHERE drawing_id = #{drawingId}")
    String selectMaxVersion(@Param("drawingId") Long drawingId);

    @Select("SELECT id, drawing_no AS drawingNo, customer_drawing_no AS customerDrawingNo, version, title, " +
            "material_grade AS materialGrade, spec_size AS specSize, unit_weight AS unitWeight, " +
            "material_code AS materialCode, " +
            "status, is_fa AS isFa, is_new AS isNew, owner_user_id AS ownerUserId, dept_id AS deptId, " +
            "created_at AS createdAt FROM crm_drawing " +
            "WHERE (#{keyword} IS NULL OR drawing_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR customer_drawing_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR title LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR material_code LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "AND (#{category} IS NULL OR title LIKE CONCAT('%', #{category}, '%')) " +
            "AND (#{isFa} IS NULL OR is_fa = #{isFa}) " +
            "AND (#{hasMaterialCode} IS NULL OR #{hasMaterialCode} = false OR (material_code IS NOT NULL AND material_code <> '')) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectDrawings6D(@Param("keyword") String keyword,
                                                @Param("status") String status,
                                                @Param("category") String category,
                                                @Param("isFa") Integer isFa,
                                                @Param("hasMaterialCode") Boolean hasMaterialCode,
                                                @Param("limit") int limit,
                                                @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM crm_drawing " +
            "WHERE (#{keyword} IS NULL OR drawing_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR customer_drawing_no LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR title LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR material_code LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "AND (#{isFa} IS NULL OR is_fa = #{isFa}) " +
            "AND (#{hasMaterialCode} IS NULL OR #{hasMaterialCode} = 0 OR (material_code IS NOT NULL AND material_code != ''))")
    long countDrawings6D(@Param("keyword") String keyword,
                          @Param("status") String status,
                          @Param("isFa") Integer isFa,
                          @Param("hasMaterialCode") Boolean hasMaterialCode);
}
