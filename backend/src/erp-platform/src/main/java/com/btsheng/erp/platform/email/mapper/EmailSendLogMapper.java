package com.btsheng.erp.platform.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.email.entity.EmailSendLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmailSendLogMapper extends BaseMapper<EmailSendLog> {

    @Select("SELECT * FROM email_send_log " +
            "WHERE (#{status} IS NULL OR #{status} = '' OR status = #{status}) " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<EmailSendLog> selectPage(@Param("status") String status,
                                  @Param("limit") int limit,
                                  @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM email_send_log " +
            "WHERE (#{status} IS NULL OR #{status} = '' OR status = #{status})")
    long countByStatus(@Param("status") String status);
}
