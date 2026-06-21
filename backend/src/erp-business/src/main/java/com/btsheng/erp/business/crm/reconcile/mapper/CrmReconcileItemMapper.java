package com.btsheng.erp.business.crm.reconcile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmReconcileItemMapper extends BaseMapper<CrmReconcileItem> {

    @Select("SELECT * FROM crm_reconcile_item WHERE reconcile_id = #{reconcileId} ORDER BY sort, id")
    List<CrmReconcileItem> selectByReconcileId(@Param("reconcileId") Long reconcileId);
}
