package com.btsheng.erp.core.integration;

/** 跨服务集成 Redis Stream 常量 */
public final class IntegrationStreams {

    public static final String STREAM_INTEGRATION = "stream:integration";

    public static final String EVENT_USER_AVAILABILITY_CHANGED = "USER_AVAILABILITY_CHANGED";
    public static final String EVENT_APPROVAL_CREATED = "APPROVAL_CREATED";
    public static final String EVENT_APPROVAL_APPROVED = "APPROVAL_APPROVED";
    public static final String EVENT_APPROVAL_REJECTED = "APPROVAL_REJECTED";
    /** 触发 MRP 重算（工序分配 / 销售审批 / 入库等） */
    public static final String EVENT_MRP_TRIGGER = "MRP_TRIGGER";

    private IntegrationStreams() {}
}
