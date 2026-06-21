package com.btsheng.erp.core.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解（V1.3.7 · BR-8）
 *
 * <p>标注 Service / Mapper 方法后由 {@link DataScopeInterceptor} 自动注入 4 级数据范围：
 * <ul>
 *   <li>{@code SELF}   → WHERE creator_id = #{currentUserId}</li>
 *   <li>{@code DEPT}   → WHERE dept_id = #{currentUserDeptId} OR 子部门</li>
 *   <li>{@code ALL}    → 无附加条件</li>
 *   <li>{@code CUSTOM} → 查 {@code sys_role_custom_dept}（V1.3.7 兜底 ALL）</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 表别名（默认 u） */
    String userAlias() default "u";

    /** 部门表别名（默认 d） */
    String deptAlias() default "d";

    /** 创建人列名（默认 creator_id） */
    String creatorColumn() default "creator_id";

    /** 部门列名（默认 dept_id） */
    String deptColumn() default "dept_id";
}
