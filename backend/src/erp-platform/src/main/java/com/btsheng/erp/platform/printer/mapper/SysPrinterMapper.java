package com.btsheng.erp.platform.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.printer.entity.SysPrinter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印机 Mapper（V1.3.9 Sprint 12 · Story 12.2 · AC-12.2.1/12.2.2）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Mapper
public interface SysPrinterMapper extends BaseMapper<SysPrinter> {

    /**
     * 多维过滤分页查询
     */
    @Select("""
            <script>
            SELECT * FROM sys_printer
            <where>
              tenant_id = #{tenantId}
              <if test="type != null and type != ''"> AND type = #{type} </if>
              <if test="status != null and status != ''"> AND status = #{status} </if>
              <if test="enabled != null"> AND enabled = #{enabled} </if>
            </where>
            ORDER BY id DESC
            </script>
            """)
    List<SysPrinter> selectByFilters(@Param("type") String type,
                                     @Param("status") String status,
                                     @Param("enabled") Integer enabled,
                                     @Param("tenantId") Long tenantId);

    /**
     * 查询所有 enabled=1 的 LABEL 打印机（心跳扫描用）
     */
    @Select("SELECT * FROM sys_printer WHERE enabled = 1 AND type = 'LABEL'")
    List<SysPrinter> selectAllEnabledLabels();

    /**
     * 查询同类型可用打印机（用于前端打印入口）
     */
    @Select("SELECT * FROM sys_printer WHERE type = #{type} AND enabled = 1 AND tenant_id = #{tenantId} ORDER BY id ASC")
    List<SysPrinter> selectAvailableByType(@Param("type") String type, @Param("tenantId") Long tenantId);

    /**
     * name 唯一性校验（创建时）
     */
    @Select("SELECT COUNT(*) FROM sys_printer WHERE name = #{name}")
    int countByName(@Param("name") String name);

    /**
     * name 唯一性校验（更新时排除自己）
     */
    @Select("SELECT COUNT(*) FROM sys_printer WHERE name = #{name} AND id != #{excludeId}")
    int countByNameExcludingId(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 心跳结果更新（单台）
     */
    @Update("UPDATE sys_printer SET status = #{status}, fail_count = #{failCount}, " +
            "last_heartbeat_at = #{lastHeartbeatAt}, updated_at = NOW() WHERE id = #{id}")
    int updateHeartbeat(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("failCount") Integer failCount,
                        @Param("lastHeartbeatAt") LocalDateTime lastHeartbeatAt);

    /**
     * 计数（按过滤条件）
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_printer
            <where>
              tenant_id = #{tenantId}
              <if test="type != null and type != ''"> AND type = #{type} </if>
              <if test="status != null and status != ''"> AND status = #{status} </if>
              <if test="enabled != null"> AND enabled = #{enabled} </if>
            </where>
            </script>
            """)
    long countByFilters(@Param("type") String type,
                        @Param("status") String status,
                        @Param("enabled") Integer enabled,
                        @Param("tenantId") Long tenantId);
}
