package com.btsheng.erp.platform.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.core.dict.entity.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictMapper extends BaseMapper<Dict> {

    @Select("SELECT * FROM sys_dict WHERE dict_type = #{dictType} AND status != 'DELETED' ORDER BY sort ASC")
    List<Dict> selectActiveByType(@Param("dictType") String dictType);

    @Select("<script>SELECT * FROM sys_dict WHERE status != 'DELETED' "
            + "<if test='dictType != null and dictType != \"\"'> AND dict_type = #{dictType}</if> "
            + "ORDER BY dict_type ASC, sort ASC</script>")
    List<Dict> selectActive(@Param("dictType") String dictType);

    @Select("<script>SELECT COUNT(*) FROM sys_dict WHERE status != 'DELETED' "
            + "<if test='dictType != null and dictType != \"\"'> AND dict_type = #{dictType}</if></script>")
    long countActive(@Param("dictType") String dictType);

    @Select("SELECT COUNT(*) FROM sys_dict WHERE dict_type = #{dictType} AND dict_code = #{dictCode} AND status != 'DELETED'")
    int countByTypeAndCode(@Param("dictType") String dictType, @Param("dictCode") String dictCode);
}
