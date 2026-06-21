package com.btsheng.erp.production.outsource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceItemMapper extends BaseMapper<CrmOutsourceItem> {
    @Select("SELECT * FROM crm_outsource_item WHERE outsource_no = #{outsourceNo} ORDER BY item_seq")
    List<CrmOutsourceItem> selectByOutsourceNo(@Param("outsourceNo") String outsourceNo);
}
