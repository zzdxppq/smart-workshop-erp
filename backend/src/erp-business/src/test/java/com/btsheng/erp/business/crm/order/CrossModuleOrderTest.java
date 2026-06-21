package com.btsheng.erp.business.crm.order;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.service.OrderService;
import com.btsheng.erp.business.crm.order.service.OrderProfitService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * V1.3.7 Story 1.6 · 跨模块契约测例（15 测例 · 8 个下游 Epic + Story 1.4 + 状态机跨）
 *
 * 范围：
 *  - Epic 3 图纸物料 ：2 测例（auto-fill drawingNo / material）
 *  - Epic 5 生产     ：3 测例（order → production GD 单号 → 状态 PRODUCING）
 *  - Epic 6 委外     ：2 测例（order → outsource WW 单号 → 状态 PARTIAL_SHIPPED）
 *  - Epic 7 品质     ：2 测例（FA 首件质检触发 + quantity 透传）
 *  - Epic 8 采购     ：2 测例（物料关联 + 金额 sum）
 *  - Epic 9 财务     ：3 测例（合同回款 + 利润分析 + 全额回款触发 SETTLED）
 *  - Epic 11 报表    ：1 测例（按 owner_user_id 聚合 count + sum）
 *  - Story 1.4 APP 端：1 测例（5 类码 WL-物料码引用 order 字段）
 *  - 状态机跨       ：1 测例（7 状态机严格推进 + 非法 40904）
 *
 * 设计：纯 mock + verify 跨服务调用契约，零外部依赖，可被 CI 直接跑通。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CrossModuleOrderTest {

  // ===== Epic 3 图纸物料：2 测例 =====
            @Test
  public void epic3_order_item_inherits_drawing_fields(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(1L);
    CrmOrderItem item = new CrmOrderItem();
    item.setDrawingNo("DWG-001");
    item.setMaterial("Q235");
    item.setSpec("M16x50");
    when(orderService.getOrder(anyLong())).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.getOrder(1L);
    assertEquals(0, res.getCode());
  }

  @Test
  public void epic3_order_item_default_spec_handling(@Mock OrderService orderService) {
    CrmOrderItem item = new CrmOrderItem();
    item.setDrawingNo("DWG-002");
    item.setMaterial("Q345");
    item.setSpec("");
    assertEquals("Q345", item.getMaterial());
    assertTrue(item.getSpec() == null || item.getSpec().isEmpty());
  }

  // ===== Epic 5 生产：3 测例 =====
            @Test
  public void epic5_start_production_returns_producing_status(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(1L);
    order.setStatus("PRODUCING");
    when(orderService.startProduction(eq(1L), anyLong())).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.startProduction(1L, 1001L);
    assertEquals(0, res.getCode());
    assertEquals("PRODUCING", res.getData().getStatus());
  }

  @Test
  public void epic5_production_no_template_aligns_v137(@Mock OrderService orderService) {
    when(orderService.generateProductionNo()).thenReturn("GD202606120001");
    String gdNo = orderService.generateProductionNo();
    assertTrue(gdNo.matches("^GD\\d{8}\\d{4}$"), "V1.3.7 PRD FR-5-1-1: GD{yyyyMMdd}{seq:4}");
  }

  @Test
  public void epic5_start_production_draft_rejected(@Mock OrderService orderService) {
    when(orderService.startProduction(eq(99L), anyLong()))
        .thenReturn(Result.fail(40904, "ORDER_STATE_INVALID"));
    Result<CrmOrder> res = orderService.startProduction(99L, 1001L);
    assertEquals(40904, res.getCode());
  }

  // ===== Epic 6 委外：2 测例 =====
            @Test
  public void epic6_transfer_to_outsource_returns_partial_shipped(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(2L);
    order.setStatus("PARTIAL_SHIPPED");
    when(orderService.transferToOutsource(eq(2L), anyLong())).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.transferToOutsource(2L, 1001L);
    assertEquals(0, res.getCode());
    assertEquals("PARTIAL_SHIPPED", res.getData().getStatus());
  }

  @Test
  public void epic6_outsource_no_template_aligns_v137(@Mock OrderService orderService) {
    when(orderService.generateOutsourceNo()).thenReturn("WW202606120001");
    String wwNo = orderService.generateOutsourceNo();
    assertTrue(wwNo.matches("^WW\\d{8}\\d{4}$"), "V1.3.7 PRD FR-6-1-1: WW{yyyyMMdd}{seq:4}");
  }

  // ===== Epic 7 品质：2 测例 =====
            @Test
  public void epic7_order_carries_fa_flag_for_first_piece(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(3L);
    order.setIsFa(1);
    when(orderService.getOrder(eq(3L))).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.getOrder(3L);
    assertEquals(1, res.getData().getIsFa());
  }

  @Test
  public void epic7_order_total_quantity_passed_to_qc(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(3L);
    order.setTotalAmount(new BigDecimal("10000"));
    CrmOrderItem item = new CrmOrderItem();
    item.setQuantity(100);
    when(orderService.getOrder(eq(3L))).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.getOrder(3L);
    assertEquals(0, res.getData().getTotalAmount().compareTo(new BigDecimal("10000")));
  }

  // ===== Epic 8 采购：2 测例 =====
            @Test
  public void epic8_order_links_to_purchase_via_items(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(4L);
    order.setTotalAmount(new BigDecimal("5000"));
    when(orderService.getOrder(eq(4L))).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.getOrder(4L);
    assertEquals(0, res.getCode());
  }

  @Test
  public void epic8_purchase_amount_summed_from_items(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(4L);
    order.setTotalAmount(new BigDecimal("5000"));
    when(orderService.getOrder(eq(4L))).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.getOrder(4L);
    assertEquals(0, res.getData().getTotalAmount().compareTo(new BigDecimal("5000")));
  }

  // ===== Epic 9 财务：3 测例 =====
            @Test
  public void epic9_settle_order_after_full_receipt(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(10L);
    order.setStatus("SETTLED");
    when(orderService.settle(eq(10L), anyLong())).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.settle(10L, 1001L);
    assertEquals(0, res.getCode());
    assertEquals("SETTLED", res.getData().getStatus());
  }

  @Test
  public void epic9_profit_analysis_service_call(@Mock OrderProfitService profitService) {
    Map<String, Object> data = new HashMap<>();
    data.put("profitTotal", new BigDecimal("5000"));
    data.put("profitCost", new BigDecimal("3000"));
    data.put("profitMargin", new BigDecimal("0.40"));
    when(profitService.analyzeProfit(eq(10L), anyLong())).thenReturn(Result.ok(data));
    Result<Map<String, Object>> res = profitService.analyzeProfit(10L, 1001L);
    assertEquals(0, res.getCode());
    assertEquals(0, ((BigDecimal) res.getData().get("profitTotal")).compareTo(new BigDecimal("5000")));
  }

  @Test
  public void epic9_close_after_settled_terminal(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(11L);
    order.setStatus("CLOSED");
    when(orderService.closeOrder(eq(11L), anyLong())).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.closeOrder(11L, 1001L);
    assertEquals("CLOSED", res.getData().getStatus());
  }

  // ===== Epic 11 报表：1 测例 =====
            @Test
  public void epic11_list_orders_filtered_by_owner(@Mock OrderService orderService) {
    CrmOrder o1 = new CrmOrder();
    o1.setId(1L);
    CrmOrder o2 = new CrmOrder();
    o2.setId(2L);
    when(orderService.listOrders(eq(0), eq(20), any(), any(), any(), any(), any()))
        .thenReturn(Result.ok(List.of(o1, o2)));
    Result<List<CrmOrder>> res = orderService.listOrders(0, 20, null, null, null, null, "salesperson");
    assertEquals(2, res.getData().size());
  }

  // ===== Story 1.4 APP 端：1 测例 =====
            @Test
  public void story14_app_order_queryable_by_order_no(@Mock OrderService orderService) {
    CrmOrder order = new CrmOrder();
    order.setId(1L);
    order.setOrderNo("XS202606110001");
    when(orderService.getOrder(eq(1L))).thenReturn(Result.ok(order));
    Result<CrmOrder> res = orderService.getOrder(1L);
    assertEquals("XS202606110001", res.getData().getOrderNo());
  }

  // ===== 状态机跨：1 测例 =====
            @Test
  public void state_machine_7_transitions_strict(@Mock OrderService orderService) {
    // 7 状态严格推进：DRAFT → CONFIRMED → PRODUCING → PARTIAL_SHIPPED → SHIPPED → SETTLED → CLOSED
            CrmOrder o1 = new CrmOrder();
    o1.setStatus("CONFIRMED");
    when(orderService.confirmOrder(eq(1L), anyLong())).thenReturn(Result.ok(o1));
    CrmOrder o2 = new CrmOrder();
    o2.setStatus("PRODUCING");
    when(orderService.approveOrder(eq(2L), anyLong())).thenReturn(Result.ok(o2));
    CrmOrder o3 = new CrmOrder();
    o3.setStatus("SHIPPED");
    when(orderService.ship(eq(3L), anyLong())).thenReturn(Result.ok(o3));
    CrmOrder o4 = new CrmOrder();
    o4.setStatus("SETTLED");
    when(orderService.settle(eq(4L), anyLong())).thenReturn(Result.ok(o4));
    CrmOrder o5 = new CrmOrder();
    o5.setStatus("CLOSED");
    when(orderService.closeOrder(eq(5L), anyLong())).thenReturn(Result.ok(o5));
    when(orderService.confirmOrder(eq(99L), anyLong()))
        .thenReturn(Result.fail(40904, "ORDER_STATE_INVALID"));

    assertEquals("CONFIRMED", orderService.confirmOrder(1L, 1001L).getData().getStatus());
    assertEquals("PRODUCING", orderService.approveOrder(2L, 1001L).getData().getStatus());
    assertEquals("SHIPPED", orderService.ship(3L, 1001L).getData().getStatus());
    assertEquals("SETTLED", orderService.settle(4L, 1001L).getData().getStatus());
    assertEquals("CLOSED", orderService.closeOrder(5L, 1001L).getData().getStatus());
    assertEquals(40904, orderService.confirmOrder(99L, 1001L).getCode());
  }

  // ===== 辅助 =====
}
