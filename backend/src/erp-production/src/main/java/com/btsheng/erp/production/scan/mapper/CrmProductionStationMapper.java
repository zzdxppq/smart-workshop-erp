package com.btsheng.erp.production.scan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.scan.entity.CrmProductionStation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmProductionStationMapper extends BaseMapper<CrmProductionStation> {
    @Select("SELECT * FROM crm_production_station WHERE workorder_no = #{wo} ORDER BY transferred_at DESC")
    List<CrmProductionStation> selectByWorkorder(@Param("wo") String workorderNo);
}
