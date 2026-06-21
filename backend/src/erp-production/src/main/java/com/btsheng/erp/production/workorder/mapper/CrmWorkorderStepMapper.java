package com.btsheng.erp.production.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmWorkorderStepMapper extends BaseMapper<CrmWorkorderStep> {
    @Select("SELECT * FROM crm_workorder_step WHERE workorder_id = #{workorderId} ORDER BY step_no")
    List<CrmWorkorderStep> selectByWorkorderId(@Param("workorderId") Long workorderId);
}
