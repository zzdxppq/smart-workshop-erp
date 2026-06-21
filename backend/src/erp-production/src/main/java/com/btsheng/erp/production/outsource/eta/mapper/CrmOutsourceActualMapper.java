package com.btsheng.erp.production.outsource.eta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.production.outsource.eta.entity.CrmOutsourceActual;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmOutsourceActualMapper extends BaseMapper<CrmOutsourceActual> {

    @Select("SELECT * FROM crm_outsource_actual WHERE supplier_id = #{supplierId} " +
            "AND process_name = #{processName} ORDER BY actual_date DESC LIMIT #{limit}")
    List<CrmOutsourceActual> selectBySupplierAndProcess(@Param("supplierId") Long supplierId,
                                                         @Param("processName") String processName,
                                                         @Param("limit") int limit);

    @Select("SELECT * FROM crm_outsource_actual WHERE supplier_id = #{supplierId} ORDER BY actual_date DESC LIMIT #{limit}")
    List<CrmOutsourceActual> selectBySupplier(@Param("supplierId") Long supplierId, @Param("limit") int limit);

    @Select("SELECT * FROM crm_outsource_actual WHERE outsource_id = #{outsourceId} ORDER BY actual_date DESC")
    List<CrmOutsourceActual> selectByOutsourceId(@Param("outsourceId") Long outsourceId);
}
