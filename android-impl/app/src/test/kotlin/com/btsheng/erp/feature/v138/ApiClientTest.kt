package com.btsheng.erp.feature.v138

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * V1.3.8 Sprint 8 Story 8.5 · ApiClient DTO 字段验证测例
 *
 * <p>纯 JVM 测例（不需要 Android 设备），验证 Retrofit DTO 字段映射。
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
class ApiClientTest {

    // ===== 3.1 BatchCreateResponse =====

    @Test
    fun `batchCreateResponse fields mapping`() {
        val resp = BatchCreateResponse(
            batches = listOf(BatchInfo("BATCH-20260613-0001", 5001L, 60)),
            poStatusAfter = "PARTIAL_ARRIVED",
            qualityOrders = listOf("LJ-20260613-0001")
        )

        assertEquals(1, resp.batches!!.size)
        assertEquals("BATCH-20260613-0001", resp.batches!![0].batchNo)
        assertEquals(5001L, resp.batches!![0].materialId)
        assertEquals(60, resp.batches!![0].quantity)
        assertEquals("PARTIAL_ARRIVED", resp.poStatusAfter)
        assertEquals(1, resp.qualityOrders!!.size)
        assertEquals("LJ-20260613-0001", resp.qualityOrders!![0])
    }

    @Test
    fun `batchInfo 字段映射`() {
        val info = BatchInfo("BATCH-20260613-0099", 5999L, 999)
        assertEquals("BATCH-20260613-0099", info.batchNo)
        assertEquals(5999L, info.materialId)
        assertEquals(999, info.quantity)
    }

    @Test
    fun `batchCreateRequest 字段映射`() {
        val req = BatchCreateRequest(
            poId = 1001L,
            arrivedAt = java.time.LocalDateTime.of(2026, 6, 13, 10, 30),
            items = listOf(BatchItem(5001L, 60))
        )

        assertEquals(1001L, req.poId)
        assertEquals(2026, req.arrivedAt.year)
        assertEquals(1, req.items.size)
        assertEquals(5001L, req.items[0].materialId)
    }

    // ===== 3.2 MaterialBarcodeParseResponse =====

    @Test
    fun `materialBarcodeParseResponse 字段映射`() {
        val resp = MaterialBarcodeParseResponse(
            materialId = 5001L,
            materialNo = "WL-A001",
            batchId = 8001L,
            batchNo = "BATCH-20260613-0001",
            arrivedAt = java.time.LocalDateTime.of(2026, 6, 13, 10, 30),
            qualityStatus = "PENDING"
        )

        assertEquals(5001L, resp.materialId)
        assertEquals("WL-A001", resp.materialNo)
        assertEquals(8001L, resp.batchId)
        assertEquals("BATCH-20260613-0001", resp.batchNo)
        assertEquals("PENDING", resp.qualityStatus)
    }

    // ===== 4.1 NoOrderPurchaseResponse =====

    @Test
    fun `noOrderPurchaseResponse 字段映射`() {
        val resp = NoOrderPurchaseResponse(
            poId = 8008L,
            poNo = "PO-20260613-0008",
            sourceType = "NO_ORDER",
            purchaseReason = "URGENT_REPLENISH",
            approvalRoute = "PROCUREMENT_MANAGER",
            estimatedTotal = 4000.0
        )

        assertEquals(8008L, resp.poId)
        assertEquals("PO-20260613-0008", resp.poNo)
        assertEquals("NO_ORDER", resp.sourceType)
        assertEquals("URGENT_REPLENISH", resp.purchaseReason)
        assertEquals("PROCUREMENT_MANAGER", resp.approvalRoute)
        assertEquals(4000.0, resp.estimatedTotal)
    }

    @Test
    fun `purchaseReason 4 项枚举`() {
        val codes = listOf("URGENT_REPLENISH", "CUSTOMER_ADD", "STOCK_SWAP", "OTHER")
        val names = listOf("紧急补料", "客户加单", "库存置换", "其他")
        val colors = listOf("red", "orange", "blue", "gray")

        for (i in codes.indices) {
            val reason = PurchaseReason(codes[i], names[i], colors[i])
            assertEquals(codes[i], reason.code)
            assertEquals(names[i], reason.name)
            assertEquals(colors[i], reason.color)
        }
    }

    // ===== 4.2 ApprovalRouteResponse =====

    @Test
    fun `approvalRouteResponse 字段映射`() {
        val resp = ApprovalRouteResponse(
            route = listOf("PROCUREMENT_MANAGER"),
            matchedThresholds = listOf("AMOUNT_10K_50K"),
            estimatedSigners = 1,
            compatibleLegacyRoute = listOf("DEPT_MANAGER")
        )

        assertEquals(1, resp.route.size)
        assertEquals("PROCUREMENT_MANAGER", resp.route[0])
        assertEquals(1, resp.matchedThresholds.size)
        assertEquals("AMOUNT_10K_50K", resp.matchedThresholds[0])
        assertEquals(1, resp.estimatedSigners)
        assertNotNull(resp.compatibleLegacyRoute)
        assertEquals("DEPT_MANAGER", resp.compatibleLegacyRoute!![0])
    }

    @Test
    fun `approvalRouteResponse 兼容 legacy 可空`() {
        val resp = ApprovalRouteResponse(
            route = emptyList(),
            matchedThresholds = listOf("AMOUNT_BELOW_10K"),
            estimatedSigners = 0,
            compatibleLegacyRoute = null
        )

        assertEquals(0, resp.route.size)
        assertEquals(0, resp.estimatedSigners)
        assertNull(resp.compatibleLegacyRoute)
    }

    // ===== 4.3 GmSummaryResponse =====

    @Test
    fun `gmSummaryResponse 字段映射`() {
        val resp = GmSummaryResponse(
            period = "LAST_30D",
            noOrderPoCount = 12,
            noOrderPoAmount = 186500.0,
            urgentReplenishCount = 5,
            amountThresholdPassedRate = 0.87,
            procurementManagerWorkload = 23,
            outsourceCostRatio = 0.18
        )

        assertEquals("LAST_30D", resp.period)
        assertEquals(12, resp.noOrderPoCount)
        assertEquals(186500.0, resp.noOrderPoAmount)
        assertEquals(5, resp.urgentReplenishCount)
        assertEquals(0.87, resp.amountThresholdPassedRate)
        assertEquals(23, resp.procurementManagerWorkload)
        assertEquals(0.18, resp.outsourceCostRatio)
    }
}