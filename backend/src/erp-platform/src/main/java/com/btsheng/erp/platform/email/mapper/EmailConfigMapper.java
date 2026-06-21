package com.btsheng.erp.platform.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.email.entity.EmailConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmailConfigMapper extends BaseMapper<EmailConfig> {

    @Select("SELECT * FROM email_config WHERE id = 1 LIMIT 1")
    EmailConfig selectSingleton();
}
