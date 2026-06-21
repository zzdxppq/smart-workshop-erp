package com.btsheng.erp.platform.audit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AuditLogArchiveMapper {
    @Select("SELECT * FROM sys_audit_log_archive WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") Long id);

    @Select("SELECT * FROM sys_audit_log_archive WHERE ts < #{cutoff} LIMIT #{batchSize}")
    List<Map<String, Object>> findOlderThan(@Param("cutoff") String cutoff, @Param("batchSize") int batchSize);
}
