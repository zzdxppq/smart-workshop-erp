package com.btsheng.erp.business.crm.quality.pickup.service;

import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickup;
import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickupItem;
import com.btsheng.erp.business.crm.quality.pickup.mapper.CrmQualityPickupItemMapper;
import com.btsheng.erp.business.crm.quality.pickup.mapper.CrmQualityPickupMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 Story 1.51 测例补全 · V1.3.8 Sprint 8 Story 8.2
 *
 * <p>为 QualityPickupService 补全 18 测例，覆盖：
 * <ul>
 *   <li>AD-3 单一 163 邮箱红线（5 测例）</li>
 *   <li>领料单唯一性 P1 修补（3 测例）</li>
 *   <li>MAX_ITEM_PER_PICKUP 上限（3 测例）</li>
 *   <li>getPickup 查询（3 测例）</li>
 *   <li>inspectPickup 质检（4 测例）</li>
 * </ul>
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@DisplayName("Story 1.51 · QualityPickupService 单元测例（Sprint 8 补全）")
class QualityPickupServiceTest {

    private CrmQualityPickupMapper pickupMapper;
    private CrmQualityPickupItemMapper itemMapper;
    private DocNoGenerator docNoGenerator;
    private QualityPickupService service;

    @BeforeEach
    void setup() {
        pickupMapper = mock(CrmQualityPickupMapper.class);
        itemMapper = mock(CrmQualityPickupItemMapper.class);
        docNoGenerator = mock(DocNoGenerator.class);
        service = new QualityPickupService(pickupMapper, itemMapper, docNoGenerator);

        when(docNoGenerator.nextQualityPickupNo()).thenReturn("LP-20260613-0001");
        when(pickupMapper.insert(any(CrmQualityPickup.class))).thenReturn(1);
        when(itemMapper.insert(any(CrmQualityPickupItem.class))).thenReturn(1);
    }

    // ===== AD-3 单一 163 邮箱（5 测例） =====
            @Test
    @DisplayName("AD-3.a 163 邮箱创建成功")
    void create_163_email_success() {
        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-001", 1001L, "李品管", "苏州精机", "li@163.com", makeItems(3, "PENDING"));

        assertTrue(r.isSuccess() || r.getCode() == 0);
        assertNotNull(r.getData());
        assertEquals("LP-20260613-0001", ((CrmQualityPickup) r.getData().get("pickup")).getPickupNo());
    }

    @Test
    @DisplayName("AD-3.b 非 163 邮箱拒绝")
    void create_reject_non_163_email() {
        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-002", 1001L, "李品管", "苏州精机", "li@gmail.com", makeItems(3, "PENDING"));

        assertEquals(40009, r.getCode());
        assertTrue(r.getMessage().contains("163"));
    }

    @Test
    @DisplayName("AD-3.c null 邮箱拒绝")
    void create_reject_null_email() {
        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-003", 1001L, "李品管", "苏州精机", null, makeItems(3, "PENDING"));

        assertEquals(40009, r.getCode());
    }

    @Test
    @DisplayName("AD-3.d 邮箱子串含 163 但不是 @163.com 拒绝")
    void create_email_substring_rejected() {
        // endsWith("@163.com") 严格匹配
            Result<Map<String, Object>> r = service.createPickup(
                "SCAN-004", 1001L, "李品管", "苏州精机", "li@x.163.com.cn", makeItems(3, "PENDING"));

        assertEquals(40009, r.getCode());
    }

    @Test
    @DisplayName("AD-3.e 126/qq/outlook 等其他邮箱拒绝")
    void create_reject_other_email_providers() {
        for (String email : new String[]{"li@126.com", "li@qq.com", "li@outlook.com", "li@hotmail.com"}) {
            Result<Map<String, Object>> r = service.createPickup(
                    "SCAN-" + email.hashCode(), 1001L, "李品管", "苏州精机", email, makeItems(2, "PENDING"));
            assertEquals(40009, r.getCode(), "应拒绝 " + email);
        }
    }

    // ===== 领料单唯一 P1 修补 1（3 测例） =====
            @Test
    @DisplayName("P1-1.a scanNo 已存在拒绝")
    void create_reject_duplicate_scan_no() {
        CrmQualityPickup existing = new CrmQualityPickup();
        existing.setPickupNo("LP-OLD");
        when(pickupMapper.selectByScanNo("SCAN-DUP")).thenReturn(existing);

        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-DUP", 1001L, "李品管", "苏州精机", "li@163.com", makeItems(2, "PENDING"));

        assertEquals(40011, r.getCode());
        assertTrue(r.getMessage().contains("EXISTS"));
    }

    @Test
    @DisplayName("P1-1.b scanNo 不重复首次创建成功")
    void create_unique_scan_no_success() {
        when(pickupMapper.selectByScanNo("SCAN-NEW")).thenReturn(null);

        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-NEW", 1001L, "李品管", "苏州精机", "li@163.com", makeItems(2, "PENDING"));

        assertTrue(r.isSuccess() || r.getCode() == 0);
    }

    @Test
    @DisplayName("P1-1.c selectByScanNo 被调用 1 次")
    void create_calls_select_by_scan_no_once() {
        when(pickupMapper.selectByScanNo("SCAN-VERIFY")).thenReturn(null);

        service.createPickup("SCAN-VERIFY", 1001L, "李品管", "苏州精机", "li@163.com", makeItems(1, "PENDING"));

        verify(pickupMapper, times(1)).selectByScanNo("SCAN-VERIFY");
    }

    // ===== items 校验（3 测例） =====
            @Test
    @DisplayName("ITEMS.a items 为空拒绝")
    void create_reject_empty_items() {
        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-EMPTY", 1001L, "李品管", "苏州精机", "li@163.com", new ArrayList<>());

        assertEquals(40010, r.getCode());
    }

    @Test
    @DisplayName("ITEMS.b items 超 50 上限拒绝")
    void create_reject_over_50_items() {
        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-OVER", 1001L, "李品管", "苏州精机", "li@163.com", makeItems(51, "PENDING"));

        assertEquals(40010, r.getCode());
    }

    @Test
    @DisplayName("ITEMS.c items 正好 50 接受")
    void create_exactly_50_items_accept() {
        Result<Map<String, Object>> r = service.createPickup(
                "SCAN-50", 1001L, "李品管", "苏州精机", "li@163.com", makeItems(50, "PENDING"));

        assertTrue(r.isSuccess() || r.getCode() == 0);
    }

    // ===== getPickup 查询（3 测例） =====
            @Test
    @DisplayName("GET.a 按 pickupNo 找到")
    void get_pickup_found() {
        CrmQualityPickup p = new CrmQualityPickup();
        p.setPickupNo("LP-20260613-0001");
        p.setInspectStatus("PENDING");
        when(pickupMapper.selectByNo("LP-20260613-0001")).thenReturn(p);

        Result<Map<String, Object>> r = service.getPickup("LP-20260613-0001");

        assertTrue(r.isSuccess() || r.getCode() == 0);
    }

    @Test
    @DisplayName("GET.b 按 pickupNo 找不到返回 404")
    void get_pickup_not_found() {
        when(pickupMapper.selectByNo("LP-NOT-EXIST")).thenReturn(null);

        Result<Map<String, Object>> r = service.getPickup("LP-NOT-EXIST");

        // V1.3.7 QualityPickupService 返回 40406 (PICKUP_NOT_FOUND)
            assertEquals(40406, r.getCode());
    }

    @Test
    @DisplayName("GET.c selectByNo 被调用 1 次")
    void get_pickup_calls_select_by_no_once() {
        when(pickupMapper.selectByNo("LP-X")).thenReturn(null);

        service.getPickup("LP-X");

        verify(pickupMapper, times(1)).selectByNo("LP-X");
    }

    // ===== inspectPickup 质检（4 测例） =====
            @Test
    @DisplayName("INSPECT.a 质检完成更新 pass/fail 计数")
    void inspect_updates_pass_fail() {
        CrmQualityPickup existing = new CrmQualityPickup();
        existing.setPickupNo("LP-001");
        existing.setTotalCount(10);
        existing.setInspectStatus("PENDING");
        when(pickupMapper.selectByNo("LP-001")).thenReturn(existing);

        Result<Map<String, Object>> r = service.inspectPickup("LP-001", makeItems(10, "PASS"));

        assertTrue(r.isSuccess() || r.getCode() == 0);

        ArgumentCaptor<CrmQualityPickup> captor = ArgumentCaptor.forClass(CrmQualityPickup.class);
        verify(pickupMapper).updateById(captor.capture());
        CrmQualityPickup updated = captor.getValue();
        assertEquals(10, updated.getPassCount());
        assertEquals(0, updated.getFailCount());
    }

    @Test
    @DisplayName("INSPECT.b 混合 PASS/FAIL 计数正确")
    void inspect_mixed_pass_fail() {
        CrmQualityPickup existing = new CrmQualityPickup();
        existing.setPickupNo("LP-MIX");
        existing.setTotalCount(5);
        existing.setInspectStatus("PENDING");
        when(pickupMapper.selectByNo("LP-MIX")).thenReturn(existing);

        // 3 PASS + 2 FAIL
            List<CrmQualityPickupItem> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) results.add(makeItem(5000L + i, "PASS"));
        for (int i = 0; i < 2; i++) results.add(makeItem(5010L + i, "FAIL"));

        Result<Map<String, Object>> r = service.inspectPickup("LP-MIX", results);

        assertTrue(r.isSuccess() || r.getCode() == 0);

        ArgumentCaptor<CrmQualityPickup> captor = ArgumentCaptor.forClass(CrmQualityPickup.class);
        verify(pickupMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getPassCount());
        assertEquals(2, captor.getValue().getFailCount());
    }

    @Test
    @DisplayName("INSPECT.c 质检完成 → 状态 INSPECTED")
    void inspect_status_updated_to_inspected() {
        CrmQualityPickup existing = new CrmQualityPickup();
        existing.setTotalCount(5);
        existing.setInspectStatus("PENDING");
        when(pickupMapper.selectByNo("LP-ALL")).thenReturn(existing);

        Result<Map<String, Object>> r = service.inspectPickup("LP-ALL", makeItems(5, "PASS"));

        assertTrue(r.isSuccess() || r.getCode() == 0);

        ArgumentCaptor<CrmQualityPickup> captor = ArgumentCaptor.forClass(CrmQualityPickup.class);
        verify(pickupMapper).updateById(captor.capture());
        assertEquals("INSPECTED", captor.getValue().getInspectStatus());
    }

    @Test
    @DisplayName("INSPECT.d 质检找不到 pickup 404")
    void inspect_not_found() {
        when(pickupMapper.selectByNo("LP-NOT-EXIST")).thenReturn(null);

        Result<Map<String, Object>> r = service.inspectPickup("LP-NOT-EXIST", makeItems(1, "PASS"));

        assertEquals(40406, r.getCode());
    }

    // ===== 辅助 =====
            private List<CrmQualityPickupItem> makeItems(int count, String inspectResult) {
        List<CrmQualityPickupItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(makeItem((long) (5000 + i), inspectResult));
        }
        return items;
    }

    private CrmQualityPickupItem makeItem(Long materialId, String inspectResult) {
        CrmQualityPickupItem item = new CrmQualityPickupItem();
        item.setMaterialCode("WL-" + materialId);
        item.setQuantity(10);
        item.setInspectResult(inspectResult);
        return item;
    }
}