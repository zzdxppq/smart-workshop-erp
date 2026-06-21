package com.btsheng.erp.business.finance.signedscan.mapper;

import com.btsheng.erp.business.finance.signedscan.dto.SignedScanArchiveVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SignedScanArchiveMapper {

    @Select("<script>SELECT s.id, s.reconcile_id, s.signer_name, s.signed_at, s.signature_image_path, "
            + "r.reconcile_no, r.vendor_name, r.period_year, r.period_month "
            + "FROM crm_reconcile_signature s JOIN crm_reconcile r ON r.id = s.reconcile_id "
            + "<where><if test='keyword != null and keyword != \"\"'>"
            + "AND (r.reconcile_no LIKE CONCAT('%', #{keyword}, '%') OR r.vendor_name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR s.signer_name LIKE CONCAT('%', #{keyword}, '%'))</if></where> "
            + "ORDER BY s.signed_at DESC LIMIT #{offset}, #{limit}</script>")
    List<SignedScanArchiveVo> selectPage(@Param("keyword") String keyword,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM crm_reconcile_signature s JOIN crm_reconcile r ON r.id = s.reconcile_id "
            + "<where><if test='keyword != null and keyword != \"\"'>"
            + "AND (r.reconcile_no LIKE CONCAT('%', #{keyword}, '%') OR r.vendor_name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR s.signer_name LIKE CONCAT('%', #{keyword}, '%'))</if></where></script>")
    long count(@Param("keyword") String keyword);
}
