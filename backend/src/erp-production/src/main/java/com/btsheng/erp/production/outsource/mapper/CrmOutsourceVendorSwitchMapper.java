package com.btsheng.erp.production.outsource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceVendorSwitch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceVendorSwitchMapper extends BaseMapper<CrmOutsourceVendorSwitch> {

    @Select("<script>SELECT * FROM crm_outsource_vendor_switch WHERE 1=1 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (switch_no LIKE CONCAT('%',#{keyword},'%') OR outsource_no LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + " ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<CrmOutsourceVendorSwitch> selectPage(@Param("keyword") String keyword,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM crm_outsource_vendor_switch WHERE 1=1 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (switch_no LIKE CONCAT('%',#{keyword},'%') OR outsource_no LIKE CONCAT('%',#{keyword},'%'))"
            + "</if></script>")
    long countByKeyword(@Param("keyword") String keyword);
}
