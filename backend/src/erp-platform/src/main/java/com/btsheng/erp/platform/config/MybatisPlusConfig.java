package com.btsheng.erp.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.btsheng.erp.core.web.AesGcmTypeHandler;
import com.btsheng.erp.core.web.DataScopeInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（V1.3.7）
 *
 * <p>注册 AesGcmTypeHandler（phone 加密）、DataScopeInterceptor（4 级数据权限）、
 * 乐观锁插件、分页插件。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public DataScopeInterceptor dataScopeInterceptor() {
        return new DataScopeInterceptor();
    }

    public TypeHandler<?>[] typeHandlers() {
        return new TypeHandler<?>[]{new AesGcmTypeHandler()};
    }
}
