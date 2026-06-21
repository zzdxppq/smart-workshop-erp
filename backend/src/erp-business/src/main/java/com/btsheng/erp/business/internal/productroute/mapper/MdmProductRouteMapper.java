package com.btsheng.erp.business.internal.productroute.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.internal.productroute.entity.MdmProductRoute;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MdmProductRouteMapper extends BaseMapper<MdmProductRoute> {

    @Select("SELECT * FROM mdm_product_route WHERE product_code = #{productCode} ORDER BY process_seq")
    List<MdmProductRoute> selectByProductCode(String productCode);

    @Delete("DELETE FROM mdm_product_route WHERE product_code = #{productCode}")
    int deleteByProductCode(String productCode);
}
