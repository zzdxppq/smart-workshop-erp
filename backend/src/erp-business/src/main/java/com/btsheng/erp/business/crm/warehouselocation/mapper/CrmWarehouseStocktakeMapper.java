package com.btsheng.erp.business.crm.warehouselocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseStocktake;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWarehouseStocktakeMapper extends BaseMapper<CrmWarehouseStocktake> {

    @Select("SELECT * FROM crm_warehouse_stocktake ORDER BY created_at DESC LIMIT 200")
    List<CrmWarehouseStocktake> selectRecent();
}
