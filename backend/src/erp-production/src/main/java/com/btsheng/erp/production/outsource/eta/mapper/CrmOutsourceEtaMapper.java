package com.btsheng.erp.production.outsource.eta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceEta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmOutsourceEtaMapper extends BaseMapper<CrmOutsourceEta> {

    @Select("SELECT * FROM crm_outsource_eta WHERE outsource_id = #{outsourceId} ORDER BY created_at DESC")
    List<CrmOutsourceEta> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_outsource_eta WHERE supplier_id = #{supplierId} ORDER BY created_at DESC LIMIT #{limit}")
    List<CrmOutsourceEta> selectBySupplier(@Param("supplierId") Long supplierId, @Param("limit") int limit);

    @Select("SELECT * FROM crm_outsource_eta WHERE eta_no = #{etaNo}")
    CrmOutsourceEta selectByEtaNo(@Param("etaNo") String etaNo);

    @Update("UPDATE crm_outsource_eta SET actual_delivery_date = #{actualDate}, deviation_pct = #{deviationPct}, " +
            "accuracy_passed = #{accuracyPassed}, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateActual(@Param("id") Long id, @Param("actualDate") java.time.LocalDate actualDate,
                     @Param("deviationPct") java.math.BigDecimal deviationPct,
                     @Param("accuracyPassed") Integer accuracyPassed,
                     @Param("status") String status);

    @Select("SELECT COUNT(*) AS total, SUM(CASE WHEN accuracy_passed=1 THEN 1 ELSE 0 END) AS passed " +
            "FROM crm_outsource_eta WHERE supplier_id = #{supplierId} AND accuracy_passed IS NOT NULL")
    Map<String, Object> selectAccuracyStats(@Param("supplierId") Long supplierId);

    @Select("<script>SELECT * FROM crm_outsource_eta WHERE 1=1 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (outsource_no LIKE CONCAT('%',#{keyword},'%') OR supplier_name LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + " ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<CrmOutsourceEta> selectPage(@Param("keyword") String keyword,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM crm_outsource_eta WHERE 1=1 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (outsource_no LIKE CONCAT('%',#{keyword},'%') OR supplier_name LIKE CONCAT('%',#{keyword},'%'))"
            + "</if></script>")
    long countByKeyword(@Param("keyword") String keyword);
}
