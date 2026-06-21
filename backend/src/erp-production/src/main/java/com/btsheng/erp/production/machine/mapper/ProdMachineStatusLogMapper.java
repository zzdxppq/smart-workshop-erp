package com.btsheng.erp.production.machine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.machine.entity.ProdMachineStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProdMachineStatusLogMapper extends BaseMapper<ProdMachineStatusLog> {

    @Select("SELECT * FROM prod_machine_status_log WHERE machine_id = #{machineId} ORDER BY changed_at DESC LIMIT #{limit}")
    List<ProdMachineStatusLog> selectByMachineId(@Param("machineId") Long machineId, @Param("limit") int limit);
}