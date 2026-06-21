package com.btsheng.erp.production.machine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.machine.entity.ProdMachineLoad;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProdMachineLoadMapper extends BaseMapper<ProdMachineLoad> {

    @Select("SELECT * FROM prod_machine_load WHERE machine_id = #{machineId} AND load_date = #{loadDate} LIMIT 1")
    ProdMachineLoad selectByMachineAndDate(@Param("machineId") Long machineId, @Param("loadDate") java.time.LocalDate loadDate);
}
