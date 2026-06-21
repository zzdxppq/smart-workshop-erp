package com.btsheng.erp.production.rework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.rework.entity.CrmRework;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmReworkMapper extends BaseMapper<CrmRework> {
    @Select("SELECT * FROM crm_rework WHERE outsource_id = #{outsourceId} ORDER BY created_at ASC")
    List<CrmRework> selectByOutsourceId(@Param("outsourceId") Long outsourceId);

    @Select("<script>SELECT * FROM crm_rework WHERE 1=1 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (rework_no LIKE CONCAT('%',#{keyword},'%') OR outsource_no LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + " ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<CrmRework> selectPage(@Param("keyword") String keyword,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM crm_rework WHERE 1=1 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (rework_no LIKE CONCAT('%',#{keyword},'%') OR outsource_no LIKE CONCAT('%',#{keyword},'%'))"
            + "</if></script>")
    long countByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM crm_rework WHERE outsource_id = #{outsourceId} ORDER BY created_at DESC LIMIT 1")
    CrmRework selectLatestByOutsourceId(@Param("outsourceId") Long outsourceId);
}
