package com.btsheng.erp.core.web;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 在 {@link DataScope} 标注的方法执行期间启用数据权限 SQL 过滤。
 */
@Aspect
@Component
public class DataScopeAspect {

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        DataScopeContext current = DataScopeContext.current();
        if (current == null) {
            return pjp.proceed();
        }
        DataScopeContext enabled = current.withFilterEnabled(true);
        if (dataScope.userAlias() != null && !dataScope.userAlias().isBlank()) {
            enabled = new DataScopeContext(
                    current.getUserId(),
                    current.getDeptId(),
                    current.getDataScope(),
                    dataScope.userAlias(),
                    dataScope.deptAlias(),
                    true);
        }
        DataScopeContext.bind(enabled);
        try {
            return pjp.proceed();
        } finally {
            DataScopeContext.bind(current);
        }
    }
}
