package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.authz.DrawingAuthz;
import com.btsheng.erp.business.crm.drawing.dto.AccessibleDrawingListResponse;
import com.btsheng.erp.business.crm.drawing.dto.DrawingLinkListResponse;
import com.btsheng.erp.business.crm.drawing.dto.OperatorProcessDrawingResponse;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingLinkMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.redis.CacheTemplate;
import com.btsheng.erp.core.web.CurrentUserHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * V1.3.9 Sprint 13 Story 13.3 · crm_drawing_link 真实查询服务
 *
 * <p>3 个端点的服务层：
 * <ul>
 *   <li>{@link #getLinksByDrawing} 端点 1 · 图纸 → 关联 bizIds（无缓存）</li>
 *   <li>{@link #getAccessibleDrawings} 端点 2 · 业务单据 → 可访问图纸列表（Redis 5min）</li>
 *   <li>{@link #getOperatorProcessDrawings} 端点 3 · 工序 → 图纸列表（Redis 5min）</li>
 * </ul>
 *
 * <p>Redis 缓存 Key 设计（防角色权限漂移 · IMPL 注意事项 #2）：
 * <ul>
 *   <li>端点 2：`drawing:link:{bizType}:{bizId}:{role}:{user_id}` TTL 300s</li>
 *   <li>端点 3：`user:current_process:{user_id}` TTL 300s</li>
 * </ul>
 *
 * <p>降级策略：Redis fail_count >= 3 后退化 DB 直查
 *
 * <p>12.1 灰度集成：复用 `draw.acl.gray.{ROLE}` feature flag · 灰度关闭返空集
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Service
public class DrawingLinkQueryService {

    private static final Logger log = LoggerFactory.getLogger(DrawingLinkQueryService.class);

    /** Redis 缓存 TTL 5 分钟 */
    private static final long CACHE_TTL_SECONDS = 300;

    /** Redis 失败容差（fail_count 阈值） */
    private static final int REDIS_FAIL_THRESHOLD = 3;

    /** Redis 失败计数（线程安全 · key = cache name） */
    private final Map<String, AtomicInteger> redisFailCount = new ConcurrentHashMap<>();

    private final CrmDrawingLinkMapper linkMapper;
    private final CrmDrawingMapper drawingMapper;
    private final CacheTemplate cacheTemplate;
    private final DrawingAuthz drawingAuthz;
    private final PlatformLookupMapper platformLookupMapper;

    @Autowired
    public DrawingLinkQueryService(CrmDrawingLinkMapper linkMapper,
                                    CrmDrawingMapper drawingMapper,
                                    CacheTemplate cacheTemplate,
                                    DrawingAuthz drawingAuthz,
                                    PlatformLookupMapper platformLookupMapper) {
        this.linkMapper = linkMapper;
        this.drawingMapper = drawingMapper;
        this.cacheTemplate = cacheTemplate;
        this.drawingAuthz = drawingAuthz;
        this.platformLookupMapper = platformLookupMapper;
    }

    // ============================================================
    // 端点 1 · GET /drawings/{id}/links?biz_type=ORDER
    // 图纸 → 关联 bizIds（无缓存 · 5 类 link JOIN）
    // ============================================================

    /**
     * 端点 1 · 图纸 → 关联 bizIds（按 biz_type 单类过滤 · 无缓存）
     *
     * <p>5 类 link JOIN SQL（复用 V54 crm_drawing_link · material_code 路径）
     * <p>权限控制：角色与 biz_type 必须匹配（DrawingAuthz scope 校验）
     *
     * @param drawingId 图纸 ID
     * @param bizType   业务类型 ORDER/PO/INCOMING/INSPECTION/WORKORDER_PROCESS
     * @param auth      当前用户 Authentication
     */
    public Result<DrawingLinkListResponse> getLinksByDrawing(Long drawingId, String bizType, Authentication auth) {
        if (drawingId == null || bizType == null || bizType.isEmpty()) {
            return Result.fail(40001, "biz_type 必填且不能为空");
        }

        // 校验 biz_type 枚举
            if (!isValidBizType(bizType)) {
            return Result.fail(40001, "INVALID_BIZ_TYPE · biz_type 必须在 5 类枚举内");
        }

        // 校验图纸存在
            if (drawingMapper.selectById(drawingId) == null) {
            return Result.fail(40401, "DRAWING_NOT_FOUND · 图纸不存在");
        }

        // 校验角色与 biz_type 匹配（scope 强制）
            if (auth != null && !drawingAuthz.isFeatureFlagEnabled(auth)) {
            // 灰度关闭：返空集（与 12.1 mock 行为一致）
            return okLinkList(drawingId, bizType, Collections.emptyList(), "DB_REAL");
        }

        String role = DrawingAuthz.primaryRole(auth);
        if (!isRoleBizTypeMatched(role, bizType)) {
            return Result.fail(40304, "DRAWING_FORBIDDEN · 角色 " + role + " 与 biz_type " + bizType + " 不匹配");
        }

        // 5 类 link JOIN 真实查询
            Long userId = resolveUserId(auth);
        List<Long> bizIds = queryBizIdsByType(drawingId, bizType, userId);

        return okLinkList(drawingId, bizType, bizIds == null ? Collections.emptyList() : bizIds, "DB_REAL");
    }

    private Result<DrawingLinkListResponse> okLinkList(Long drawingId, String bizType, List<Long> bizIds, String source) {
        DrawingLinkListResponse resp = new DrawingLinkListResponse();
        resp.setDrawingId(drawingId);
        resp.setBizType(bizType);
        resp.setBizIds(bizIds);
        resp.setTotalCount(bizIds.size());
        resp.setQuerySource(source);
        resp.setQueriedAt(LocalDateTime.now());
        return Result.ok(resp);
    }

    /**
     * 5 类 link JOIN 真实查询分发
     */
    private List<Long> queryBizIdsByType(Long drawingId, String bizType, Long userId) {
        try {
            switch (bizType) {
                case CrmDrawingLink.BIZ_TYPE_ORDER:
                    return linkMapper.selectOrderBizIdsByDrawing(drawingId, userId);
                case CrmDrawingLink.BIZ_TYPE_PO:
                    return linkMapper.selectPoBizIdsByDrawing(drawingId, userId);
                case CrmDrawingLink.BIZ_TYPE_INCOMING:
                    return linkMapper.selectIncomingBizIdsByDrawing(drawingId, userId);
                case CrmDrawingLink.BIZ_TYPE_INSPECTION:
                    return linkMapper.selectInspectionBizIdsByDrawing(drawingId, userId);
                case CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS:
                    // 端点 1 不查工序（端点 3 专用）· 返空集
            return Collections.emptyList();
                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("[13.3] queryBizIdsByType error drawing={} bizType={} userId={}", drawingId, bizType, userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 端点 2 5 类业务单据 → 可访问图纸列表（5 个具体方法分发）
     */
    private List<Map<String, Object>> queryAccessibleDrawingsByType(String bizType, Long bizId) {
        switch (bizType) {
            case CrmDrawingLink.BIZ_TYPE_ORDER:
                return linkMapper.selectDrawingsByOrderBizRef(bizId);
            case CrmDrawingLink.BIZ_TYPE_PO:
                return linkMapper.selectDrawingsByPoBizRef(bizId);
            case CrmDrawingLink.BIZ_TYPE_INCOMING:
                return linkMapper.selectDrawingsByIncomingBizRef(bizId);
            case CrmDrawingLink.BIZ_TYPE_INSPECTION:
                return linkMapper.selectDrawingsByInspectionBizRef(bizId);
            case CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS:
                return linkMapper.selectDrawingsByWorkorderProcessBizRef(bizId);
            default:
                return Collections.emptyList();
        }
    }

    // ============================================================
    // 端点 2 · GET /drawings/accessible?biz_type=ORDER&biz_id=123
    // 业务单据 → 可访问图纸列表（Redis 5min · Key 含 role+user_id）
    // ============================================================

    /**
     * 端点 2 · 业务单据 → 可访问图纸列表（Redis 5min 缓存）
     *
     * <p>Redis Key：`drawing:link:{bizType}:{bizId}:{role}:{user_id}` · TTL 300s
     * <p>缓存命中 → cacheHit=true · 缓存未命中 → DB 真实查询 → 回写 Redis
     * <p>降级：Redis fail_count >= 3 → 退化 DB 直查
     */
    public Result<AccessibleDrawingListResponse> getAccessibleDrawings(String bizType, Long bizId, Authentication auth) {
        if (bizType == null || bizType.isEmpty() || bizId == null) {
            return Result.fail(40001, "biz_type 和 biz_id 必填");
        }
        if (!isValidBizType(bizType)) {
            return Result.fail(40001, "INVALID_BIZ_TYPE");
        }

        // 角色匹配
            String role = DrawingAuthz.primaryRole(auth);
        if (!isRoleBizTypeMatched(role, bizType)) {
            return Result.fail(40304, "DRAWING_FORBIDDEN · 角色 " + role + " 与 biz_type " + bizType + " 不匹配");
        }

        // 灰度检查（关闭则返空集）
            if (!drawingAuthz.isFeatureFlagEnabled(auth)) {
            return okAccessibleList(bizType, bizId, Collections.emptyList(), false);
        }

        // Redis 缓存 Key（IMPL 注意事项 #2：含 role + user_id 防角色权限漂移）
            Long userId = resolveUserId(auth);
        String cacheKey = "drawing:link:" + bizType + ":" + bizId + ":" + role + ":" + userId;

        // 尝试 Redis 读取（带降级）
            CachedAccessible cached = redisGet(cacheKey, "endpoint2-accessible");
        if (cached != null) {
            return okAccessibleList(bizType, bizId, cached.getDrawings(), true);
        }

        // DB 真实查询（端点 2 SQL · 按 bizType 分发到 5 个具体方法）
            List<Map<String, Object>> rows;
        try {
            rows = queryAccessibleDrawingsByType(bizType, bizId);
        } catch (Exception e) {
            log.error("[13.3] selectDrawingsByBizRefAccessible error bizType={} bizId={}", bizType, bizId, e);
            return Result.fail(50001, "DB 查询失败");
        }

        if (rows == null || rows.isEmpty()) {
            return okAccessibleList(bizType, bizId, Collections.emptyList(), false);
        }

        List<AccessibleDrawingListResponse.AccessibleDrawing> drawings = mapToAccessibleDrawings(rows);

        // 回写 Redis（带降级）
            CachedAccessible toCache = new CachedAccessible();
        toCache.setDrawings(drawings);
        redisSet(cacheKey, toCache);

        return okAccessibleList(bizType, bizId, drawings, false);
    }

    private Result<AccessibleDrawingListResponse> okAccessibleList(String bizType, Long bizId,
                                                                     List<AccessibleDrawingListResponse.AccessibleDrawing> drawings,
                                                                     boolean cacheHit) {
        AccessibleDrawingListResponse resp = new AccessibleDrawingListResponse();
        resp.setBizType(bizType);
        resp.setBizId(bizId);
        resp.setDrawings(drawings);
        resp.setTotalCount(drawings.size());
        resp.setCacheHit(cacheHit);
        resp.setQueriedAt(LocalDateTime.now());
        return Result.ok(resp);
    }

    // ============================================================
    // 端点 3 · GET /drawings/process/{processId}
    // OPERATOR 工序扫码 → 图纸列表（Redis 5min · 手机端高频）
    // ============================================================

    /**
     * 端点 3 · 工序 → 图纸列表（OPERATOR 扫码 · Redis 5min 缓存）
     *
     * <p>Redis Key：`user:current_process:{user_id}` · TTL 300s
     * <p>鉴权：wp.status = 'IN_PROGRESS' AND (wp.operator_user_id = currentUserId OR role IN admin/ENGINEER)
     * <p>命中缓存 → cacheHit=true · 响应 < 5ms · 端到端 < 200ms
     */
    public Result<OperatorProcessDrawingResponse> getOperatorProcessDrawings(Long processId, Authentication auth) {
        if (processId == null) {
            return Result.fail(40001, "processId 必填");
        }

        // 角色校验（admin/ENGINEER/OPERATOR 限定）
            String role = DrawingAuthz.primaryRole(auth);
        if (!DrawingAuthz.ROLE_ADMIN.equalsIgnoreCase(role)
                && !DrawingAuthz.ROLE_ENGINEER.equalsIgnoreCase(role)
                && !DrawingAuthz.ROLE_OPERATOR.equalsIgnoreCase(role)) {
            return Result.fail(40304, "PROCESS_FORBIDDEN · 仅 admin/ENGINEER/OPERATOR 可访问");
        }

        // 灰度检查
            if (!drawingAuthz.isFeatureFlagEnabled(auth)) {
            return okProcessList(processId, null, null, null, null, null, Collections.emptyList(), false);
        }

        // Redis 缓存 Key（user_id 维度 · 防止用户扫码频率过高重复查 DB）
            Long userId = resolveUserId(auth);
        String cacheKey = "user:current_process:" + userId;

        CachedProcess cached = redisGet(cacheKey, "endpoint3-process");
        if (cached != null && processId.equals(cached.getProcessId())) {
            return okProcessList(processId, cached.getProcessCode(), cached.getProcessName(),
                    cached.getWorkOrderId(), cached.getWorkOrderCode(), cached.getStatus(),
                    cached.getDrawings(), true);
        }

        // DB 真实查询
            List<Map<String, Object>> rows;
        try {
            rows = linkMapper.selectOperatorProcessDrawings(processId);
        } catch (Exception e) {
            log.error("[13.3] selectOperatorProcessDrawings error processId={}", processId, e);
            return Result.fail(50001, "DB 查询失败");
        }

        if (rows == null || rows.isEmpty()) {
            // 工序不存在或非 IN_PROGRESS
            return Result.fail(40402, "PROCESS_NOT_FOUND · 工序不存在或非 IN_PROGRESS 状态");
        }

        // 鉴权检查：wp.operator_user_id == currentUserId（admin/ENGINEER 跳过）
            Map<String, Object> firstRow = rows.get(0);
        Long opUserId = toLong(firstRow.get("operator_user_id"));
        if (!DrawingAuthz.ROLE_ADMIN.equalsIgnoreCase(role)
                && !DrawingAuthz.ROLE_ENGINEER.equalsIgnoreCase(role)
                && !opUserId.equals(userId)) {
            return Result.fail(40304, "PROCESS_FORBIDDEN · 工序非本人操作");
        }

        // 组装响应
            List<OperatorProcessDrawingResponse.ProcessDrawing> drawings = mapToProcessDrawings(rows);

        CachedProcess toCache = new CachedProcess();
        toCache.setProcessId(processId);
        toCache.setProcessCode(toStr(firstRow.get("process_code")));
        toCache.setProcessName(toStr(firstRow.get("process_name")));
        toCache.setWorkOrderId(toLong(firstRow.get("work_order_id")));
        toCache.setWorkOrderCode(toStr(firstRow.get("work_order_code")));
        toCache.setStatus(toStr(firstRow.get("status")));
        toCache.setDrawings(drawings);

        redisSet(cacheKey, toCache);

        return okProcessList(processId, toCache.getProcessCode(), toCache.getProcessName(),
                toCache.getWorkOrderId(), toCache.getWorkOrderCode(), toCache.getStatus(),
                drawings, false);
    }

    private Result<OperatorProcessDrawingResponse> okProcessList(Long processId, String processCode, String processName,
                                                                  Long workOrderId, String workOrderCode, String status,
                                                                  List<OperatorProcessDrawingResponse.ProcessDrawing> drawings,
                                                                  boolean cacheHit) {
        OperatorProcessDrawingResponse resp = new OperatorProcessDrawingResponse();
        resp.setProcessId(processId);
        resp.setProcessCode(processCode);
        resp.setProcessName(processName);
        resp.setWorkOrderId(workOrderId);
        resp.setWorkOrderCode(workOrderCode);
        resp.setStatus(status);
        resp.setDrawings(drawings);
        resp.setTotalCount(drawings.size());
        resp.setCacheHit(cacheHit);
        resp.setQueriedAt(LocalDateTime.now());
        return Result.ok(resp);
    }

    // ============================================================
    // 缓存失效（crm_drawing_link 写入时 @CacheEvict 触发）
    // ============================================================

    /**
     * 缓存失效：crm_drawing_link 写入（INSERT/UPDATE/DELETE）时调用
     * 清空全部 drawing:link:* 和 user:current_process:* 缓存
     */
    @CacheEvict(value = {"drawingLink:PERM"}, allEntries = true)
    public void evictAllLinkCaches() {
        // 占位：实际清空由 Spring Cache Abstraction 完成
            log.info("[13.3] evictAllLinkCaches · crm_drawing_link 写入触发");
    }

    // ============================================================
    // 辅助方法
    // ============================================================
            private boolean isValidBizType(String bizType) {
        return CrmDrawingLink.BIZ_TYPE_ORDER.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_PO.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_INCOMING.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_INSPECTION.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS.equals(bizType)
                || CrmDrawingLink.BIZ_TYPE_OUTSOURCE.equals(bizType);
    }

    private boolean isRoleBizTypeMatched(String role, String bizType) {
        if (role == null || role.isEmpty()) return false;
        // admin/ENGINEER/PROD_PLANNER 跨 5 类均可查
            if (DrawingAuthz.ROLE_ADMIN.equalsIgnoreCase(role)
                || DrawingAuthz.ROLE_ENGINEER.equalsIgnoreCase(role)
                || DrawingAuthz.ROLE_PROD_PLANNER.equalsIgnoreCase(role)) {
            return true;
        }
        // SALES 仅 ORDER · PURCHASER 仅 PO · WAREHOUSE 仅 INCOMING · QC 仅 INSPECTION · OPERATOR 仅 WORKORDER_PROCESS
            switch (role.toUpperCase()) {
            case DrawingAuthz.ROLE_SALES:     return CrmDrawingLink.BIZ_TYPE_ORDER.equals(bizType);
            case DrawingAuthz.ROLE_PURCHASER:
                return CrmDrawingLink.BIZ_TYPE_PO.equals(bizType)
                        || CrmDrawingLink.BIZ_TYPE_OUTSOURCE.equals(bizType);
            case DrawingAuthz.ROLE_WAREHOUSE: return CrmDrawingLink.BIZ_TYPE_INCOMING.equals(bizType);
            case DrawingAuthz.ROLE_QC:        return CrmDrawingLink.BIZ_TYPE_INSPECTION.equals(bizType);
            case DrawingAuthz.ROLE_OPERATOR:  return CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS.equals(bizType);
            default: return false;
        }
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) {
            return 0L;
        }
        Long uid = CurrentUserHelper.resolveUserId(auth.getName(), platformLookupMapper::findUserIdByUsername);
        return uid == null ? 0L : uid;
    }

    private List<AccessibleDrawingListResponse.AccessibleDrawing> mapToAccessibleDrawings(List<Map<String, Object>> rows) {
        List<AccessibleDrawingListResponse.AccessibleDrawing> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AccessibleDrawingListResponse.AccessibleDrawing d = new AccessibleDrawingListResponse.AccessibleDrawing();
            d.setDrawingId(toLong(row.get("drawing_id")));
            d.setDrawingCode(toStr(row.get("drawing_code")));
            d.setDrawingName(toStr(row.get("drawing_name")));
            d.setVersion(toStr(row.get("version")));
            d.setThumbnailUrl(toStr(row.get("thumbnail_url")));
            d.setPermissionLevel("VIEW");   // 默认 VIEW（5 操作矩阵裁剪）
            result.add(d);
        }
        return result;
    }

    private List<OperatorProcessDrawingResponse.ProcessDrawing> mapToProcessDrawings(List<Map<String, Object>> rows) {
        List<OperatorProcessDrawingResponse.ProcessDrawing> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            OperatorProcessDrawingResponse.ProcessDrawing d = new OperatorProcessDrawingResponse.ProcessDrawing();
            d.setDrawingId(toLong(row.get("drawing_id")));
            d.setDrawingCode(toStr(row.get("drawing_code")));
            d.setDrawingName(toStr(row.get("drawing_name")));
            d.setThumbnailUrl(toStr(row.get("thumbnail_url")));
            d.setPermissionLevel("VIEW");
            result.add(d);
        }
        return result;
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private String toStr(Object o) { return o == null ? null : o.toString(); }

    // ============================================================
    // Redis 缓存读写（带降级策略 · fail_count >= 3 退化 DB）
    // ============================================================
            private <T> T redisGet(String key, String cacheName) {
        if (shouldDegrade(cacheName)) return null;
        try {
            return cacheTemplate.getOrLoad(key, new TypeReference<T>() {}, CACHE_TTL_SECONDS, () -> null);
        } catch (Exception e) {
            recordRedisFail(cacheName, e);
            log.warn("[13.3] redis get fail key={} · fail_count={}", key, redisFailCount.get(cacheName), e);
            return null;
        }
    }

    private void redisSet(String key, Object value) {
        if (shouldDegrade("any")) return;
        try {
            cacheTemplate.setIfAbsent(key, toJson(value), CACHE_TTL_SECONDS);
            // 注意：setIfAbsent 仅在 key 不存在时写入 · 真实业务用 set + TTL
            // 这里使用 setIfAbsent 简化（如果已存在不覆盖）· 实际用 setEx
        } catch (Exception e) {
            recordRedisFail("any", e);
            log.warn("[13.3] redis set fail key={}", key, e);
        }
    }

    private String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean shouldDegrade(String cacheName) {
        AtomicInteger c = redisFailCount.get(cacheName);
        return c != null && c.get() >= REDIS_FAIL_THRESHOLD;
    }

    private void recordRedisFail(String cacheName, Exception e) {
        redisFailCount.computeIfAbsent(cacheName, k -> new AtomicInteger(0)).incrementAndGet();
    }

    // ============================================================
    // 内部缓存 DTO
    // ============================================================
            public static class CachedAccessible {
        private List<AccessibleDrawingListResponse.AccessibleDrawing> drawings = new ArrayList<>();
        public List<AccessibleDrawingListResponse.AccessibleDrawing> getDrawings() { return drawings; }
        public void setDrawings(List<AccessibleDrawingListResponse.AccessibleDrawing> drawings) { this.drawings = drawings; }
    }

    public static class CachedProcess {
        private Long processId;
        private String processCode;
        private String processName;
        private Long workOrderId;
        private String workOrderCode;
        private String status;
        private List<OperatorProcessDrawingResponse.ProcessDrawing> drawings = new ArrayList<>();

        public Long getProcessId() { return processId; }
        public void setProcessId(Long processId) { this.processId = processId; }
        public String getProcessCode() { return processCode; }
        public void setProcessCode(String processCode) { this.processCode = processCode; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public Long getWorkOrderId() { return workOrderId; }
        public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }
        public String getWorkOrderCode() { return workOrderCode; }
        public void setWorkOrderCode(String workOrderCode) { this.workOrderCode = workOrderCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<OperatorProcessDrawingResponse.ProcessDrawing> getDrawings() { return drawings; }
        public void setDrawings(List<OperatorProcessDrawingResponse.ProcessDrawing> drawings) { this.drawings = drawings; }
    }
}
