package com.btsheng.erp.core.web;

import com.btsheng.erp.core.web.AuthException;
import com.btsheng.erp.core.web.PermException;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Properties;

/**
 * 数据权限 SQL 拦截器（V1.3.7 · BR-8）
 *
 * <p>在 {@code @DataScope} 标注的方法中，根据当前用户角色的 data_scope 自动拼接 WHERE 条件。
 * 拦截 {@link StatementHandler#prepare} 阶段。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataScopeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql();
        if (!sql.toUpperCase().contains("SELECT")) {
            return invocation.proceed();
        }
        // V1.3.7 简化：从 ThreadLocal / SecurityContext 取当前用户数据范围
        DataScopeContext ctx = DataScopeContext.current();
        if (ctx == null || !ctx.isFilterEnabled()) {
            return invocation.proceed();
        }
        String scope = ctx.getDataScope();
        if (scope == null || scope.isBlank() || "ALL".equalsIgnoreCase(scope)) {
            return invocation.proceed();
        }
        String userAlias = ctx.getUserAlias() == null ? "u" : ctx.getUserAlias();
        String deptAlias = ctx.getDeptAlias() == null ? "d" : ctx.getDeptAlias();
        if (!sqlContainsAlias(sql, userAlias)) {
            return invocation.proceed();
        }
        String append;
        switch (scope) {
            case "SELF":
                append = " AND " + userAlias + ".creator_id = " + ctx.getUserId();
                break;
            case "DEPT":
                append = " AND " + userAlias + ".dept_id = " + ctx.getDeptId();
                break;
            case "ALL":
            case "CUSTOM":
            default:
                append = "";
        }
        if (!append.isEmpty()) {
            String newSql = appendWhere(sql, append);
            MetaObject boundSqlMeta = SystemMetaObject.forObject(boundSql);
            boundSqlMeta.setValue("sql", newSql);
            log.debug("[DataScope] {} -> appended: {}", scope, append);
        }
        return invocation.proceed();
    }

    /** 仅当 SQL 中实际使用了表别名（如 u.dept_id）时才注入数据权限，避免污染 sys_* 单表查询 */
    private static boolean sqlContainsAlias(String sql, String alias) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        return sql.matches("(?is).*\\b" + java.util.regex.Pattern.quote(alias) + "\\.");
    }

    private String appendWhere(String sql, String append) {
        String lower = sql.toLowerCase();
        int idx = lower.lastIndexOf("where");
        if (idx < 0) {
            int groupBy = lower.lastIndexOf("group by");
            int orderBy = lower.lastIndexOf("order by");
            int insertAt = sql.length();
            if (groupBy > 0) insertAt = groupBy;
            if (orderBy > 0 && orderBy < insertAt) insertAt = orderBy;
            return sql.substring(0, insertAt) + " WHERE 1=1" + append + sql.substring(insertAt);
        }
        return sql.substring(0, idx + "WHERE".length()) + " (1=1)" + append + sql.substring(idx + "WHERE".length());
    }

    @Override
    public Object plugin(Object target) {
        return org.apache.ibatis.plugin.Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
