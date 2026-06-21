package com.btsheng.erp.business.crm.reconcile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrmReconcileMapper extends BaseMapper<CrmReconcile> {

    @Select("SELECT * FROM crm_reconcile WHERE reconcile_no = #{reconcileNo} LIMIT 1")
    CrmReconcile selectByReconcileNo(@Param("reconcileNo") String reconcileNo);

    @Select("SELECT * FROM crm_reconcile WHERE id = #{id} LIMIT 1")
    CrmReconcile selectById(@Param("id") Long id);

    @Select("SELECT * FROM crm_reconcile " +
            "WHERE (#{vendorId} IS NULL OR vendor_id = #{vendorId}) " +
            "AND (#{periodYear} IS NULL OR period_year = #{periodYear}) " +
            "AND (#{periodMonth} IS NULL OR period_month = #{periodMonth}) " +
            "AND (#{status} IS NULL OR status = #{status}) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectReconciles(@Param("vendorId") Long vendorId,
                                               @Param("periodYear") Integer periodYear,
                                               @Param("periodMonth") Integer periodMonth,
                                               @Param("status") String status,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);
}
