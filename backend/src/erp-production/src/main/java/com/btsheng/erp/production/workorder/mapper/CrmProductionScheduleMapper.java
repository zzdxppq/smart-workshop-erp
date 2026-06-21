package com.btsheng.erp.production.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.workorder.entity.CrmProductionSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmProductionScheduleMapper extends BaseMapper<CrmProductionSchedule> {

    @Select("SELECT * FROM crm_production_schedule WHERE workorder_id = #{workorderId} LIMIT 1")
    CrmProductionSchedule selectByWorkorderId(@Param("workorderId") Long workorderId);

    @Select("SELECT * FROM crm_production_schedule WHERE equipment_id = #{equipmentId} " +
            "AND plan_start < #{end} AND plan_end > #{start} " +
            "AND status IN ('PLANNED', 'IN_PROGRESS')")
    List<CrmProductionSchedule> selectConflicts(@Param("equipmentId") Long equipmentId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
