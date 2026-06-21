package com.btsheng.erp.platform.audit.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AuditLogMapper {

    @Select("SELECT * FROM sys_audit_log WHERE ts < #{cutoff} ORDER BY ts ASC LIMIT #{batchSize}")
    List<Map<String, Object>> selectOldData(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);

    @Insert("INSERT INTO sys_audit_log_archive (id, user_id, module, action, before_json, after_json, ip, ts, archived_at) " +
            "VALUES (#{id}, #{userId}, #{module}, #{action}, #{beforeJson}, #{afterJson}, #{ip}, #{ts}, NOW())")
    int archive(Map<String, Object> record);

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_audit_log WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT id, user_id AS userId, module, action, before_json AS beforeJson, " +
            "after_json AS afterJson, ip, ts FROM sys_audit_log ORDER BY ts DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_audit_log")
    long countAll();

    /**
     * 归档 1 年前数据（默认 365 天）
     * @param retentionDays 保留天数
     * @return 归档条数
     */
    default int archiveOldData(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Map<String, Object>> old = selectOldData(cutoff, 1000);
        int count = 0;
        for (Map<String, Object> r : old) {
            archive(r);
            deleteById(((Number) r.get("id")).longValue());
            count++;
        }
        return count;
    }
}
