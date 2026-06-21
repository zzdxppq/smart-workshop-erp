package com.btsheng.erp.core.web;

/**
 * 数据权限上下文（V1.3.7 · ThreadLocal 透传）
 *
 * <p>由拦截器 / 切面在请求开始时绑定，请求结束清除。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class DataScopeContext {

    private static final ThreadLocal<DataScopeContext> HOLDER = new ThreadLocal<>();

    private final Long userId;
    private final Long deptId;
    private final String dataScope;
    private final String userAlias;
    private final String deptAlias;
    private final boolean filterEnabled;

    public DataScopeContext(Long userId, Long deptId, String dataScope, String userAlias, String deptAlias) {
        this(userId, deptId, dataScope, userAlias, deptAlias, false);
    }

    public DataScopeContext(Long userId, Long deptId, String dataScope, String userAlias, String deptAlias,
                            boolean filterEnabled) {
        this.userId = userId;
        this.deptId = deptId;
        this.dataScope = dataScope;
        this.userAlias = userAlias;
        this.deptAlias = deptAlias;
        this.filterEnabled = filterEnabled;
    }

    public static void bind(DataScopeContext ctx) {
        HOLDER.set(ctx);
    }

    public static DataScopeContext current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public Long getUserId() { return userId; }
    public Long getDeptId() { return deptId; }
    public String getDataScope() { return dataScope; }
    public String getUserAlias() { return userAlias; }
    public String getDeptAlias() { return deptAlias; }
    public boolean isFilterEnabled() { return filterEnabled; }

    public DataScopeContext withFilterEnabled(boolean enabled) {
        return new DataScopeContext(userId, deptId, dataScope, userAlias, deptAlias, enabled);
    }
}
