package com.btsheng.erp.platform.label.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.label.entity.LabelTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签模板 Mapper（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.1）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Mapper
public interface LabelTemplateMapper extends BaseMapper<LabelTemplate> {

    /**
     * 按 type + tenant_id 查询（SB- 返回 null · 由 service fallback 到 GD）
     */
    @Select("SELECT * FROM label_template WHERE `type` = #{type} AND tenant_id = #{tenantId} AND enabled = 1")
    LabelTemplate selectByType(@Param("type") String type, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM label_template WHERE `type` = #{type} AND tenant_id = #{tenantId}")
    LabelTemplate selectRawByType(@Param("type") String type, @Param("tenantId") Long tenantId);

    /**
     * 列同租户所有启用模板（SB 不入库 · 返回 4 行：GD/LZ/WW/WL）
     */
    @Select("SELECT * FROM label_template WHERE tenant_id = #{tenantId} AND enabled = 1 ORDER BY id ASC")
    List<LabelTemplate> selectAllEnabled(@Param("tenantId") Long tenantId);

    @Select("SELECT * FROM label_template WHERE tenant_id = #{tenantId} ORDER BY id ASC")
    List<LabelTemplate> selectAllByTenant(@Param("tenantId") Long tenantId);
}