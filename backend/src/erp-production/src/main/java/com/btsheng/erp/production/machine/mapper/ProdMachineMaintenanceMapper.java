package com.btsheng.erp.production.machine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.machine.entity.ProdMachineMaintenance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProdMachineMaintenanceMapper extends BaseMapper<ProdMachineMaintenance> {

    @Select("SELECT * FROM prod_machine_maintenance WHERE machine_id = #{machineId} ORDER BY performed_at DESC LIMIT #{limit}")
    List<ProdMachineMaintenance> selectByMachineId(@Param("machineId") Long machineId, @Param("limit") int limit);
}
