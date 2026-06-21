package com.btsheng.erp.production.rework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.rework.entity.CrmReworkHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmReworkHistoryMapper extends BaseMapper<CrmReworkHistory> {
    @Select("SELECT h.* FROM crm_rework_history h JOIN crm_rework r ON h.rework_id = r.id " +
            "WHERE r.outsource_id = #{outsourceId} ORDER BY h.changed_at ASC")
    List<CrmReworkHistory> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("SELECT * FROM crm_rework_history WHERE rework_id = #{reworkId} ORDER BY changed_at ASC")
    List<CrmReworkHistory> selectByReworkId(@Param("reworkId") Long reworkId);
}
