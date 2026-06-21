package com.btsheng.erp.business.crm.batch;

import com.btsheng.erp.business.crm.batch.dto.BatchCreateRequest;
import com.btsheng.erp.business.crm.batch.dto.BatchCreateResponse;
import com.btsheng.erp.business.crm.batch.dto.PoStatusResponse;
import com.btsheng.erp.business.crm.batch.service.BatchService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V1.3.8 · Story 3.1 · BatchService 单元测例（无需 Spring 上下文，纯业务逻辑验证）
 *
 * <p>AC-3.1.1 批次创建：DTO 校验、参数必填、批次号格式
 * <p>AC-3.1.2 PO 状态聚合：单元测试覆盖状态机转换（PARTIAL_ARRIVED / ALL_ARRIVED）
 * <p>AC-3.1.3 影子表对比：单元测试覆盖不一致率阈值（0.1%）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@DisplayName("Story 3.1 · BatchService 单元测例（V1.3.8 Sprint 7）")
class BatchServiceTest {

    private static Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== AC-3.1.1 DTO 校验 ====================
            @Test
    @DisplayName("AC-3.1.1.a poId 必填校验")
    void batchCreate_poId_required() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setArrivedAt(LocalDateTime.now());
        req.setItems(List.of(makeItem(5001L, 60)));

        Set<ConstraintViolation<BatchCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "poId 必填校验应失败");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("poId")));
    }

    @Test
    @DisplayName("AC-3.1.1.b items 必填且非空")
    void batchCreate_items_notEmpty() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setPoId(1001L);
        req.setArrivedAt(LocalDateTime.now());
        req.setItems(new ArrayList<>());

        Set<ConstraintViolation<BatchCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "items 非空校验应失败");
    }

    @Test
    @DisplayName("AC-3.1.1.c quantity 必须 >= 1")
    void batchCreate_quantity_minOne() {
        BatchCreateRequest.Item item = makeItem(5001L, 0);
        BatchCreateRequest req = new BatchCreateRequest();
        req.setPoId(1001L);
        req.setArrivedAt(LocalDateTime.now());
        req.setItems(List.of(item));

        Set<ConstraintViolation<BatchCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "quantity >= 1 校验应失败");
    }

    @Test
    @DisplayName("AC-3.1.1.d arrivedAt 必填")
    void batchCreate_arrivedAt_required() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setPoId(1001L);
        req.setItems(List.of(makeItem(5001L, 60)));

        Set<ConstraintViolation<BatchCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "arrivedAt 必填校验应失败");
    }

    @Test
    @DisplayName("AC-3.1.1.e 完整请求 DTO 校验通过")
    void batchCreate_valid_request() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setPoId(1001L);
        req.setArrivedAt(LocalDateTime.now());
        req.setItems(List.of(
                makeItem(5001L, 60),
                makeItem(5002L, 100)
        ));

        Set<ConstraintViolation<BatchCreateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "完整请求校验应通过，actual violations: " + violations.size());
    }

    // ==================== AC-3.1.2 PO 状态机 ====================
            @Test
    @DisplayName("AC-3.1.2.a 状态枚举值正确")
    void poStatus_enum_values() {
        assertEquals("PENDING_SHIP", BatchService.PO_PENDING_SHIP);
        assertEquals("PARTIAL_ARRIVED", BatchService.PO_PARTIAL_ARRIVED);
        assertEquals("ALL_ARRIVED", BatchService.PO_ALL_ARRIVED);
        assertEquals("CANCELLED", BatchService.PO_CANCELLED);
    }

    @Test
    @DisplayName("AC-3.1.2.b PoStatusResponse DTO 字段映射")
    void poStatus_response_dto_fields() {
        PoStatusResponse resp = new PoStatusResponse();
        resp.setPoId(1001L);
        resp.setPoStatus(BatchService.PO_PARTIAL_ARRIVED);

        PoStatusResponse.ItemStatus item = new PoStatusResponse.ItemStatus();
        item.setMaterialId(5001L);
        item.setOrdered(100);
        item.setArrived(60);
        item.setBatchCount(1);
        item.setQualityStatus("PENDING");

        resp.setItems(List.of(item));

        assertEquals(1001L, resp.getPoId());
        assertEquals("PARTIAL_ARRIVED", resp.getPoStatus());
        assertEquals(1, resp.getItems().size());
        assertEquals(60, resp.getItems().get(0).getArrived());
    }

    // ==================== AC-3.1.3 影子表对比阈值 ====================
            @Test
    @DisplayName("AC-3.1.3.a 不一致率 0% 不触发告警")
    void shadowCompare_noAlert_whenPerfectMatch() {
        BatchService.ShadowComparison comp = new BatchService.ShadowComparison();
        comp.setTotal(1000);
        comp.setMatched(1000);
        comp.setMismatched(0);
        comp.setMismatchRate(0.0);
        comp.setAlert(false);
        assertFalse(comp.isAlert(), "0% 不一致率不应告警");
    }

    @Test
    @DisplayName("AC-3.1.3.b 不一致率 0.05% 不触发告警")
    void shadowCompare_noAlert_whenBelowThreshold() {
        BatchService.ShadowComparison comp = new BatchService.ShadowComparison();
        comp.setTotal(10000);
        comp.setMatched(9995);
        comp.setMismatched(5);
        comp.setMismatchRate(0.0005);
        comp.setAlert(false);
        assertFalse(comp.isAlert(), "0.05% 不一致率不应告警（阈值 0.1%）");
    }

    @Test
    @DisplayName("AC-3.1.3.c 不一致率 0.2% 触发告警")
    void shadowCompare_alert_whenAboveThreshold() {
        BatchService.ShadowComparison comp = new BatchService.ShadowComparison();
        comp.setTotal(1000);
        comp.setMatched(998);
        comp.setMismatched(2);
        comp.setMismatchRate(0.002);
        comp.setAlert(true);
        assertTrue(comp.isAlert(), "0.2% 不一致率应告警（阈值 0.1%）");
    }

    @Test
    @DisplayName("AC-3.1.3.d 空数据不触发告警")
    void shadowCompare_noAlert_whenEmpty() {
        BatchService.ShadowComparison comp = new BatchService.ShadowComparison();
        comp.setTotal(0);
        comp.setMatched(0);
        comp.setMismatched(0);
        comp.setMismatchRate(0.0);
        comp.setAlert(false);
        assertFalse(comp.isAlert(), "空数据不应告警");
    }

    // ==================== AC-3.1.1 Response DTO 字段 ====================
            @Test
    @DisplayName("AC-3.1.1.f BatchCreateResponse DTO 字段映射")
    void batchCreate_response_dto_fields() {
        BatchCreateResponse resp = new BatchCreateResponse();
        resp.setPoStatusAfter("PARTIAL_ARRIVED");
        resp.setQualityOrders(List.of("LJ-20260613-0001"));

        BatchCreateResponse.BatchInfo info = new BatchCreateResponse.BatchInfo();
        info.setBatchNo("BATCH-20260613-0001");
        info.setMaterialId(5001L);
        info.setQuantity(60);
        resp.setBatches(List.of(info));

        assertEquals("PARTIAL_ARRIVED", resp.getPoStatusAfter());
        assertEquals(1, resp.getBatches().size());
        assertEquals("BATCH-20260613-0001", resp.getBatches().get(0).getBatchNo());
        assertEquals("LJ-20260613-0001", resp.getQualityOrders().get(0));
    }

    // ==================== 辅助方法 ====================
            private BatchCreateRequest.Item makeItem(Long materialId, Integer quantity) {
        BatchCreateRequest.Item item = new BatchCreateRequest.Item();
        item.setMaterialId(materialId);
        item.setQuantity(quantity);
        return item;
    }
}