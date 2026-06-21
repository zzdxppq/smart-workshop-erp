package com.btsheng.erp.production.outsource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceHistoryMapper extends BaseMapper<CrmOutsourceHistory> {
    @Select("SELECT * FROM crm_outsource_history WHERE outsource_no = #{outsourceNo} ORDER BY operated_at DESC")
    List<CrmOutsourceHistory> selectByOutsourceNo(@Param("outsourceNo") String outsourceNo);
}
