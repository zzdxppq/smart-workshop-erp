package com.btsheng.erp.production.machine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.machine.entity.ProdMachine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProdMachineMapper extends BaseMapper<ProdMachine> {

    @Select("SELECT * FROM prod_machine WHERE machine_code = #{machineCode} AND is_active = 1 LIMIT 1")
    ProdMachine selectByCode(String machineCode);

    @Select("SELECT * FROM prod_machine WHERE is_active = 1 ORDER BY machine_no, id")
    List<ProdMachine> selectActive();
}
