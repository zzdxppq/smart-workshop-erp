package com.btsheng.erp.production.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.process.entity.CrmProcessStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * V1.3.7 · Story 1.10 · 工序库 Mapper
 */
@Mapper
public interface CrmProcessStepMapper extends BaseMapper<CrmProcessStep> {

    @Select("SELECT * FROM crm_process_step WHERE process_id = #{processId} ORDER BY step_no")
    List<CrmProcessStep> selectByProcessId(Long processId);

    @Select("SELECT MAX(step_no) FROM crm_process_step WHERE process_id = #{processId}")
    Integer maxStepNo(Long processId);
}
