package com.btsheng.erp.platform.auth.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内置角色（V1.3.7 · BR-12）
 *
 * <p>9 类内置角色不可禁用/删除：业务员 / 部门经理 / 总经理 / 财务 / 采购 / 仓管 / 品检 / 生管 / 系统管理员。
 */
public enum BuiltinRoleEnum {

    SYS_ADMIN("SYS_ADMIN", "系统管理员"),
    SALES("SALES", "业务员"),
    SALES_MGR("SALES_MGR", "销售经理"),
    GM("GM", "总经理"),
    PROD_MGR("PROD_MGR", "生管"),
    ENGINEER("ENGINEER", "工程师"),
    WAREHOUSE("WAREHOUSE", "仓管"),
    QC("QC", "品检"),
    BUYER("BUYER", "采购"),
    FINANCE("FINANCE", "财务"),
    HR("HR", "人事");

    private final String code;
    private final String label;

    BuiltinRoleEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static final Set<String> CODES = Arrays.stream(values()).map(BuiltinRoleEnum::getCode).collect(Collectors.toSet());

    public static boolean isBuiltin(String code) {
        return code != null && CODES.contains(code);
    }
}
