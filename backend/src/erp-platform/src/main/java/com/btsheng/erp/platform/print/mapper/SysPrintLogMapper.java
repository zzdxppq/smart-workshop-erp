package com.btsheng.erp.platform.print.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.print.entity.SysPrintLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 打印留痕 Mapper（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.3/12.4.4）
 *
 * <p>6 索引命中：idx_print_log_operator / idx_print_log_code / idx_print_log_time
 * <p>status partial 索引 · reference partial 索引
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Mapper
public interface SysPrintLogMapper extends BaseMapper<SysPrintLog> {

    /**
     * TC-12.4.3.1/3.2 多维过滤分页查询 · 命中 idx_print_log_time + idx_print_log_code
     */
    @Select("""
            <script>
            SELECT *
            FROM sys_print_log
            <where>
              tenant_id = #{tenantId}
              <if test="codeType != null">AND code_type = #{codeType}</if>
              <if test="mode != null">AND print_mode = #{mode}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="operatorId != null">AND operator_user_id = #{operatorId}</if>
              <if test="codeValue != null and codeValue != ''">AND code_value LIKE CONCAT('%', #{codeValue}, '%')</if>
              <if test="dateFrom != null">AND printed_at >= #{dateFrom}</if>
              <if test="dateTo != null">AND printed_at &lt; #{dateTo}</if>
            </where>
            ORDER BY printed_at DESC
            <if test="limit != null and offset != null">
            LIMIT #{limit} OFFSET #{offset}
            </if>
            </script>
            """)
    List<SysPrintLog> selectByFilters(@Param("codeType") String codeType,
                                       @Param("mode") String mode,
                                       @Param("status") String status,
                                       @Param("operatorId") Long operatorId,
                                       @Param("codeValue") String codeValue,
                                       @Param("dateFrom") LocalDateTime dateFrom,
                                       @Param("dateTo") LocalDateTime dateTo,
                                       @Param("limit") Integer limit,
                                       @Param("offset") Integer offset,
                                       @Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM sys_print_log
            <where>
              tenant_id = #{tenantId}
              <if test="codeType != null">AND code_type = #{codeType}</if>
              <if test="mode != null">AND print_mode = #{mode}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="operatorId != null">AND operator_user_id = #{operatorId}</if>
              <if test="codeValue != null and codeValue != ''">AND code_value LIKE CONCAT('%', #{codeValue}, '%')</if>
              <if test="dateFrom != null">AND printed_at >= #{dateFrom}</if>
              <if test="dateTo != null">AND printed_at &lt; #{dateTo}</if>
            </where>
            </script>
            """)
    long countByFilters(@Param("codeType") String codeType,
                         @Param("mode") String mode,
                         @Param("status") String status,
                         @Param("operatorId") Long operatorId,
                         @Param("codeValue") String codeValue,
                         @Param("dateFrom") LocalDateTime dateFrom,
                         @Param("dateTo") LocalDateTime dateTo,
                         @Param("tenantId") Long tenantId);

    /**
     * TC-12.4.4.1/4.2 按 groupBy 聚合
     * groupBy=operator_id / code_type / mode / month
     */
    @Select("""
            <script>
            SELECT
              <choose>
                <when test="groupBy == 'operator_id'">CAST(operator_user_id AS CHAR) AS bucket_key, operator_user_id AS bucket_id</when>
                <when test="groupBy == 'code_type'">code_type AS bucket_key, NULL AS bucket_id</when>
                <when test="groupBy == 'mode'">print_mode AS bucket_key, NULL AS bucket_id</when>
                <when test="groupBy == 'month'">DATE_FORMAT(printed_at, '%Y-%m') AS bucket_key, NULL AS bucket_id</when>
                <when test="groupBy == 'day'">DATE_FORMAT(printed_at, '%Y-%m-%d') AS bucket_key, NULL AS bucket_id</when>
                <otherwise>DATE_FORMAT(printed_at, '%Y-%m') AS bucket_key, NULL AS bucket_id</otherwise>
              </choose>
            </script>
            """)
    List<Map<String, Object>> aggregateGroupBy(@Param("groupBy") String groupBy,
                                                 @Param("dateFrom") LocalDateTime dateFrom,
                                                 @Param("dateTo") LocalDateTime dateTo,
                                                 @Param("tenantId") Long tenantId);

    /**
     * 按 groupBy + 状态聚合（统计成功/失败）
     */
    @Select("""
            <script>
            SELECT
              <choose>
                <when test="groupBy == 'operator_id'">CAST(operator_user_id AS CHAR)</when>
                <when test="groupBy == 'code_type'">code_type</when>
                <when test="groupBy == 'mode'">print_mode</when>
                <when test="groupBy == 'month'">DATE_FORMAT(printed_at, '%Y-%m')</when>
                <when test="groupBy == 'day'">DATE_FORMAT(printed_at, '%Y-%m-%d')</when>
                <otherwise>DATE_FORMAT(printed_at, '%Y-%m')</otherwise>
              </choose>
              AS bucket_key,
              status,
              COUNT(*) AS cnt,
              SUM(copies) AS total_copies
            FROM sys_print_log
            WHERE tenant_id = #{tenantId}
              AND printed_at >= #{dateFrom} AND printed_at &lt; #{dateTo}
            GROUP BY bucket_key, status
            </script>
            """)
    List<Map<String, Object>> aggregateByGroupAndStatus(@Param("groupBy") String groupBy,
                                                           @Param("dateFrom") LocalDateTime dateFrom,
                                                           @Param("dateTo") LocalDateTime dateTo,
                                                           @Param("tenantId") Long tenantId);

    /**
     * TC-12.4.3.4 引用计数（防误删 + 12.1 引用）
     */
    @Select("""
            SELECT COUNT(*) FROM sys_print_log
            WHERE printer_id = #{printerId} AND tenant_id = #{tenantId}
            """)
    long countByPrinterId(@Param("printerId") Long printerId, @Param("tenantId") Long tenantId);

    /**
     * 生成 log_no 序列号：按 tenantId + date 计数
     */
    @Select("""
            SELECT COUNT(*) + 1
            FROM sys_print_log
            WHERE tenant_id = #{tenantId}
              AND DATE(printed_at) = DATE(#{now})
            """)
    long nextLogNoSeq(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
}
