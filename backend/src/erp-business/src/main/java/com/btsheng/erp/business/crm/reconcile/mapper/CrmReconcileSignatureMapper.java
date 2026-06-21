package com.btsheng.erp.business.crm.reconcile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileSignature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmReconcileSignatureMapper extends BaseMapper<CrmReconcileSignature> {

    @Select("SELECT * FROM crm_reconcile_signature WHERE reconcile_id = #{reconcileId} ORDER BY signed_at")
    List<CrmReconcileSignature> selectByReconcileId(@Param("reconcileId") Long reconcileId);
}
