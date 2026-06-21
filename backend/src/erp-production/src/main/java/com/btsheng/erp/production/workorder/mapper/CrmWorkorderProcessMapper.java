package com.btsheng.erp.production.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWorkorderProcessMapper extends BaseMapper<CrmWorkorderProcess> {

    @Select("SELECT * FROM crm_workorder_process WHERE workorder_id = #{workorderId} ORDER BY process_seq")
    List<CrmWorkorderProcess> selectByWorkorderId(Long workorderId);

    @Select("SELECT * FROM crm_workorder_process WHERE workorder_id = #{workorderId} AND process_seq = #{processSeq} LIMIT 1")
    CrmWorkorderProcess selectByWorkorderAndSeq(Long workorderId, Integer processSeq);

    @Select("SELECT wp.* FROM crm_workorder_process wp " +
            "JOIN crm_workorder wo ON wo.id = wp.workorder_id " +
            "JOIN crm_workorder_step ws ON ws.workorder_id = wo.id AND ws.step_no = wp.process_seq " +
            "WHERE ws.operator_user_id = #{userId} AND ws.status = 'IN_PROGRESS' " +
            "ORDER BY ws.updated_at DESC LIMIT 1")
    CrmWorkorderProcess selectByOperatorInProgressStep(Long userId);

    @Select("SELECT wp.* FROM crm_workorder_process wp " +
            "JOIN crm_workorder wo ON wo.id = wp.workorder_id " +
            "WHERE wo.status = 'IN_PROGRESS' AND wp.status IN ('IN_PROGRESS','PENDING') " +
            "ORDER BY wp.process_seq LIMIT 1")
    CrmWorkorderProcess selectDemoCurrentProcess();
}
