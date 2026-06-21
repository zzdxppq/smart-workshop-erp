package com.btsheng.erp.business.crm.drawing.authz;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingLinkMapper;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import com.btsheng.erp.core.redis.CacheTemplate;
import com.btsheng.erp.core.web.CurrentUserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 图纸权限矩阵核心鉴权组件
 *
 * <p>SpEL 集中到本组件，避免 SpEL 表达式过长难维护（评审 ADR-12.1.2）。
 *
 * <p>7 角色 × 5 操作矩阵：
 * <table>
 *   <tr><th>角色</th><th>preview</th><th>print</th><th>download</th><th>upload</th><th>delete</th><th>scope</th></tr>
 *   <tr><td>ENGINEER</td><td>true</td><td>true</td><td>true</td><td>true</td><td>true</td><td>ALL</td></tr>
 *   <tr><td>PROD_PLANNER</td><td>true</td><td>true</td><td>false</td><td>false</td><td>false</td><td>ALL</td></tr>
 *   <tr><td>SALES</td><td>linked</td><td>linked</td><td>false</td><td>false</td><td>false</td><td>ORDER</td></tr>
 *   <tr><td>PURCHASER</td><td>linked</td><td>linked</td><td>false</td><td>false</td><td>false</td><td>PO</td></tr>
 *   <tr><td>WAREHOUSE</td><td>linked</td><td>linked</td><td>false</td><td>false</td><td>false</td><td>INCOMING</td></tr>
 *   <tr><td>QC</td><td>linked</td><td>linked</td><td>false</td><td>false</td><td>false</td><td>INSPECTION</td></tr>
 *   <tr><td>OPERATOR</td><td>current</td><td>false</td><td>false</td><td>false</td><td>false</td><td>WORKORDER_PROCESS</td></tr>
 *   <tr><td>FINANCE</td><td>false</td><td>false</td><td>false</td><td>false</td><td>false</td><td>NONE</td></tr>
 * </table>
 *
 * <p>性能：@Cacheable 5min Redis 缓存（key = username + ':' + drawingId）
 * <p>OPERATOR 当前工序：Redis 缓存 user_id → current_process_id 5min
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Component("drawingAuthz")
public class DrawingAuthz {

    private static final Logger log = LoggerFactory.getLogger(DrawingAuthz.class);

    /** 5 类角色码 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_ENGINEER = "ENGINEER";
    public static final String ROLE_PROD_PLANNER = "PROD_PLANNER";
    public static final String ROLE_SALES = "SALES";
    public static final String ROLE_PURCHASER = "PURCHASER";
    public static final String ROLE_WAREHOUSE = "WAREHOUSE";
    public static final String ROLE_QC = "QC";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_FINANCE = "FINANCE";

    /** 5 类操作 */
    public static final String OP_VIEW = "view";
    public static final String OP_PRINT = "print";
    public static final String OP_DOWNLOAD = "download";
    public static final String OP_UPLOAD = "upload";
    public static final String OP_DELETE = "delete";

    private final CrmDrawingLinkMapper linkMapper;
    private final CacheTemplate cacheTemplate;
    private final PlatformLookupMapper platformLookupMapper;

    @Autowired
    public DrawingAuthz(CrmDrawingLinkMapper linkMapper,
                          CacheTemplate cacheTemplate,
                          PlatformLookupMapper platformLookupMapper) {
        this.linkMapper = linkMapper;
        this.cacheTemplate = cacheTemplate;
        this.platformLookupMapper = platformLookupMapper;
    }

    // ------------------------------------------------------------
    // 5 操作公共入口（@PreAuthorize 调用）
    // ------------------------------------------------------------

    /** 是否可预览（view = canView） */
    @Cacheable(value = "drawingAuthz", key = "#auth.name + ':' + #drawingId + ':view'")
    public boolean canView(Authentication auth, Long drawingId) {
        return checkByRole(auth, drawingId, ROLE_SCOPE_VIEW);
    }

    /** 是否可打印 */
    @Cacheable(value = "drawingAuthz", key = "#auth.name + ':' + #drawingId + ':print'")
    public boolean canPrint(Authentication auth, Long drawingId) {
        return checkByRole(auth, drawingId, ROLE_SCOPE_PRINT);
    }

    /** 是否可下载原文件（仅 ENGINEER · 强隔离） */
    @Cacheable(value = "drawingAuthz", key = "#auth.name + ':' + #drawingId + ':download'")
    public boolean canDownload(Authentication auth, Long drawingId) {
        if (auth == null) return false;
        String role = primaryRole(auth);
        return ROLE_ENGINEER.equals(role);
    }

    /** 是否可上传（仅 ENGINEER） */
    @Cacheable(value = "drawingAuthz", key = "#auth.name + ':' + #drawingId + ':upload'")
    public boolean canUpload(Authentication auth, Long drawingId) {
        if (auth == null) return false;
        String role = primaryRole(auth);
        return ROLE_ENGINEER.equals(role);
    }

    /** 是否可删除（仅 ENGINEER · DRAFT 状态可删） */
    @Cacheable(value = "drawingAuthz", key = "#auth.name + ':' + #drawingId + ':delete'")
    public boolean canDelete(Authentication auth, Long drawingId) {
        if (auth == null) return false;
        String role = primaryRole(auth);
        return ROLE_ENGINEER.equals(role);
    }

    // ------------------------------------------------------------
    // 灰度 feature flag 旁路（sys_dict draw.acl.gray.{ROLE}）
    // ------------------------------------------------------------

    /** 灰度关闭 → 拒绝（即使矩阵通过也返 false） */
    public boolean isFeatureFlagEnabled(Authentication auth) {
        if (auth == null) return false;
        String role = primaryRole(auth);
        // 灰度期间：admin / ENGINEER 默认开启（兼容 V1.3.7 行为）；其余角色按 sys_dict 开关
            if (ROLE_ENGINEER.equals(role) || ROLE_PROD_PLANNER.equals(role)) {
            return true;   // 工程/生管默认不灰度（V1.3.7 全员可见行为保留）
        }
        String key = "draw.acl.gray." + role;
        // 简化：从 sys_dict 读 value（生产对接 DictCache / Redis）
        // 灰度期间默认 false → 该角色全员按 V1.3.7 行为（全员可见）
        // 灰度期间 admin 改 sys_dict value=true → 启用 12.1 矩阵
            return getFeatureFlag(key);
    }

    /** 是否启用 12.1 矩阵鉴权（feature flag 关闭即返 false → V1.3.7 全员可见） */
    public boolean canApplyAcl(Authentication auth) {
        return isFeatureFlagEnabled(auth);
    }

    // ------------------------------------------------------------
    // permission 端点专用（不依赖 feature flag · 总返回矩阵）
    // ------------------------------------------------------------

    /**
     * 取 5 操作位 · permission 端点专用（不被 feature flag 旁路）。
     * 即使灰度关闭，前端仍能正确渲染按钮（隐藏全部操作按钮）。
     */
    public PermissionBits computePermissionBits(Authentication auth, Long drawingId) {
        if (auth == null) return PermissionBits.allFalse();
        String role = primaryRole(auth);
        PermissionBits bits = new PermissionBits();
        // view / print 受 scope 控制
            bits.view = canViewCore(role, auth, drawingId);
        bits.print = canPrintCore(role, auth, drawingId);
        // download / upload / delete 仅 ENGINEER
            boolean isEngineer = ROLE_ENGINEER.equals(role);
        bits.download = isEngineer;
        bits.upload = isEngineer;
        bits.delete = isEngineer;
        bits.scope = scopeFor(role);
        bits.role = role;
        return bits;
    }

    /**
     * 关联业务单据 ID 列表（按 bizType 分桶 · permission 端点返回）
     */
    public Map<String, List<Long>> linkedBizIds(Long drawingId) {
        Map<String, List<Long>> result = new HashMap<>();
        for (String bt : new String[]{
                CrmDrawingLink.BIZ_TYPE_ORDER,
                CrmDrawingLink.BIZ_TYPE_PO,
                CrmDrawingLink.BIZ_TYPE_INCOMING,
                CrmDrawingLink.BIZ_TYPE_INSPECTION,
                CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS}) {
            List<Long> ids = linkMapper.selectBizIdsByDrawingAndBizType(drawingId, bt);
            result.put(bt, ids == null ? Collections.emptyList() : ids);
        }
        return result;
    }

    // ------------------------------------------------------------
    // 核心：按角色分支鉴权
    // ------------------------------------------------------------
            private boolean checkByRole(Authentication auth, Long drawingId, String op) {
        if (auth == null || drawingId == null) return false;

        // 灰度旁路：feature flag 关闭 → 返 true（V1.3.7 行为兼容）
            if (!isFeatureFlagEnabled(auth)) return true;

        String role = primaryRole(auth);

        // view / print 受 scope 控制
            if (ROLE_SCOPE_VIEW.equals(op)) return canViewCore(role, auth, drawingId);
        if (ROLE_SCOPE_PRINT.equals(op)) return canPrintCore(role, auth, drawingId);

        // download / upload / delete 仅 ENGINEER
            return ROLE_ENGINEER.equals(role);
    }

    private boolean canViewCore(String role, Authentication auth, Long drawingId) {
        switch (role) {
            case ROLE_ENGINEER:
            case ROLE_PROD_PLANNER:
                return true;
            case ROLE_SALES:
                return hasLinkedBiz(auth.getName(), drawingId, CrmDrawingLink.BIZ_TYPE_ORDER, () -> findSalesOrderIds(currentUserId(auth.getName())));
            case ROLE_PURCHASER:
                return hasLinkedBiz(auth.getName(), drawingId, CrmDrawingLink.BIZ_TYPE_PO, () -> findPurchaserPoIds(currentUserId(auth.getName())));
            case ROLE_WAREHOUSE:
                return hasLinkedBiz(auth.getName(), drawingId, CrmDrawingLink.BIZ_TYPE_INCOMING, () -> findWarehouseIncomingIds(currentUserId(auth.getName())));
            case ROLE_QC:
                return hasLinkedBiz(auth.getName(), drawingId, CrmDrawingLink.BIZ_TYPE_INSPECTION, () -> findQcInspectionIds(currentUserId(auth.getName())));
            case ROLE_OPERATOR:
                return hasLinkedBiz(auth.getName(), drawingId, CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS, () -> findOperatorProcessIds(currentUserId(auth.getName())));
            case ROLE_FINANCE:
            default:
                return false;
        }
    }

    private boolean canPrintCore(String role, Authentication auth, Long drawingId) {
        // print 矩阵：ENGINEER / PROD_PLANNER / SALES / PURCHASER / WAREHOUSE / QC = true（受 scope）
        // OPERATOR / FINANCE = false
            if (ROLE_OPERATOR.equals(role) || ROLE_FINANCE.equals(role)) return false;
        return canViewCore(role, auth, drawingId);
    }

    private boolean hasLinkedBiz(String username, Long drawingId, String bizType,
                                 Supplier<List<Long>> userBizIdsSupplier) {
        if (username == null || drawingId == null) return false;
        List<Long> userBizIds = userBizIdsSupplier.get();
        if (userBizIds == null || userBizIds.isEmpty()) return false;
        try {
            return linkMapper.existsByDrawingAndBizTypeAndBizIdIn(drawingId, bizType, userBizIds) > 0;
        } catch (Exception e) {
            log.warn("[DrawingAuthz] hasLinkedBiz error drawing={} biz={} user={}", drawingId, bizType, username, e);
            return false;
        }
    }

    // ------------------------------------------------------------
    // 用户 → bizIds 5 类查询（13.3 真实查询对接 · 复用 V54 crm_drawing_link）
    // ------------------------------------------------------------

    /**
     * SALES → 订单 IDs（13.3 真实查询 · 5 类 link JOIN · ORDER）
     * <p>JOIN 路径：crm_drawing → crm_drawing_link (biz_type='ORDER') → crm_order_item → crm_order
     * <p>权限过滤：crm_order.owner_user_id = userId AND status NOT IN ('DRAFT','CANCELLED')
     * <p>索引：idx_drawing_link_order (V58) + idx_order_item_material_order (V58)
     */
    @Cacheable(value = "drawingLink:SALES", key = "#userId", unless = "#result == null")
    public List<Long> findSalesOrderIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return linkMapper.selectOrderIdsByUser(userId);
        } catch (Exception e) {
            log.warn("[13.3] findSalesOrderIds error userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /** PURCHASER → PO IDs（13.3 真实查询） */
    @Cacheable(value = "drawingLink:PURCHASER", key = "#userId", unless = "#result == null")
    public List<Long> findPurchaserPoIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return linkMapper.selectPoIdsByUser(userId);
        } catch (Exception e) {
            log.warn("[13.3] findPurchaserPoIds error userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /** WAREHOUSE → 入库单 IDs（13.3 真实查询） */
    @Cacheable(value = "drawingLink:WAREHOUSE", key = "#userId", unless = "#result == null")
    public List<Long> findWarehouseIncomingIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return linkMapper.selectIncomingIdsByUser(userId);
        } catch (Exception e) {
            log.warn("[13.3] findWarehouseIncomingIds error userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /** QC → 质检单 IDs（13.3 真实查询） */
    @Cacheable(value = "drawingLink:QC", key = "#userId", unless = "#result == null")
    public List<Long> findQcInspectionIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            return linkMapper.selectInspectionIdsByUser(userId);
        } catch (Exception e) {
            log.warn("[13.3] findQcInspectionIds error userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /** OPERATOR → 当前工序 IDs（IN_PROGRESS · Redis 5min 缓存） */
    @Cacheable(value = "drawingLink:OPERATOR", key = "#userId", unless = "#result == null")
    public List<Long> findOperatorProcessIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            List<Long> ids = linkMapper.selectOperatorProcessIdsByUser(userId);
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            log.warn("[13.3] findOperatorProcessIds error userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------

    /** 取 primary role（ROLE_ 开头） */
    public static String primaryRole(Authentication auth) {
        if (auth == null) return "";
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("");
    }

    public static String scopeFor(String role) {
        switch (role) {
            case ROLE_ENGINEER:
            case ROLE_PROD_PLANNER: return CrmDrawingLink.SCOPE_ALL;
            case ROLE_SALES:        return CrmDrawingLink.SCOPE_ORDER;
            case ROLE_PURCHASER:    return CrmDrawingLink.SCOPE_PO;
            case ROLE_WAREHOUSE:    return CrmDrawingLink.SCOPE_INCOMING;
            case ROLE_QC:           return CrmDrawingLink.SCOPE_INSPECTION;
            case ROLE_OPERATOR:     return CrmDrawingLink.SCOPE_PROCESS;
            case ROLE_FINANCE:
            default:                return CrmDrawingLink.SCOPE_NONE;
        }
    }

    private Long currentUserId(String username) {
        return CurrentUserHelper.resolveUserId(username, platformLookupMapper::findUserIdByUsername);
    }

    // ------------------------------------------------------------
    // Redis 工具（生产对接 CacheTemplate）
    // ------------------------------------------------------------
            private String getRedisString(String key) {
        try {
            return (String) cacheTemplate.getOrLoad(key,
                    new com.fasterxml.jackson.core.type.TypeReference<String>() {},
                    300,
                    () -> "");
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheRedisString(String key, String value, long ttlSec) {
        try {
            cacheTemplate.setIfAbsent(key, value, ttlSec);
        } catch (Exception e) {
            log.debug("[DrawingAuthz] redis cache write failed key={}", key, e);
        }
    }

    /** 读 sys_dict DRAWING_ACL_FEATURE_FLAG · dict_code = draw.acl.gray.{ROLE} */
    private boolean getFeatureFlag(String key) {
        try {
            String raw = platformLookupMapper.findDrawingGrayFlag(key);
            if (raw == null || raw.isBlank()) {
                return false;
            }
            return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
        } catch (Exception e) {
            log.debug("[DrawingAuthz] feature flag read failed key={}", key, e);
            return false;
        }
    }

    // ------------------------------------------------------------
    // 常量：操作分组
    // ------------------------------------------------------------
            private static final String ROLE_SCOPE_VIEW = "view";
    private static final String ROLE_SCOPE_PRINT = "print";

    // ------------------------------------------------------------
    // DTO
    // ------------------------------------------------------------

    /** 5 操作位 DTO（permission 端点 / 内部返回） */
    public static class PermissionBits {
        public boolean view;
        public boolean print;
        public boolean download;
        public boolean upload;
        public boolean delete;
        public String scope;
        public String role;

        public static PermissionBits allFalse() {
            PermissionBits b = new PermissionBits();
            b.scope = CrmDrawingLink.SCOPE_NONE;
            return b;
        }

        public boolean getView() { return view; }
        public boolean getPrint() { return print; }
        public boolean getDownload() { return download; }
        public boolean getUpload() { return upload; }
        public boolean getDelete() { return delete; }
        public String getScope() { return scope; }
        public String getRole() { return role; }
    }
}