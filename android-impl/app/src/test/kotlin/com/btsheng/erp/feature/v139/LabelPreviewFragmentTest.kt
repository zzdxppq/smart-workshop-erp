package com.btsheng.erp.feature.v139

import com.btsheng.erp.feature.v138.LabelPreviewData
import com.btsheng.erp.feature.v138.LabelPreviewRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V1.3.9 Sprint 12 Story 12.3 · LabelPreviewFragment DTO 测例 (android-impl · TC-12.3.4.2)
 *
 * <p>纯 JVM 测例（不依赖 Android 设备）· 验证 DTO 字段映射与 qr_content 纯文本契约
 *
 * @author dev agent Opus 4.8 · 2026-06-14
 */
class LabelPreviewFragmentTest {

    @Test
    fun `labelPreviewRequest passes qrContent as plain text with -P03 suffix`() {
        // TC-12.3.3.1：LZ-260613-001-P03 含 -P03 工序后缀 · 纯文本
        val req = LabelPreviewRequest(
            type = "LZ",
            data = LabelPreviewData(
                qrContent = "LZ-260613-001-P03",
                lines = listOf("LZ-260613-001-P03", "工序：P03"),
                factoryName = null
            ),
            format = "PNG"
        )
        assertEquals("LZ", req.type)
        assertEquals("LZ-260613-001-P03", req.data.qrContent)
        assertTrue(req.data.qrContent.contains("-P03"))
        assertEquals("PNG", req.format)
    }

    @Test
    fun `labelPreviewRequest factoryName passthrough (V1.3.9 multi-tenant)`() {
        // TC-12.3.5.2：tenant B 厂名 "上海测试厂"
        val req = LabelPreviewRequest(
            type = "GD",
            data = LabelPreviewData(
                qrContent = "GD-260614-001",
                lines = listOf("GD-260614-001"),
                factoryName = "上海测试厂"
            ),
            format = "PNG"
        )
        assertEquals("上海测试厂", req.data.factoryName)
    }

    @Test
    fun `fragment newInstance carries arguments`() {
        // 验证 Fragment arguments 序列化
        val fragment = LabelPreviewFragment.newInstance(
            type = "GD",
            qrContent = "GD-260614-001",
            lines = listOf("GD-260614-001", "工单：WO20260614001", "工序：P03"),
            factoryName = "昆山佰泰胜精密加工"
        )
        val args = fragment.arguments
        assertEquals("GD", args?.getString("type"))
        assertEquals("GD-260614-001", args?.getString("qrContent"))
        assertEquals("昆山佰泰胜精密加工", args?.getString("factoryName"))
        val lines = args?.getStringArrayList("lines")
        assertEquals(3, lines?.size)
        assertEquals("工单：WO20260614001", lines?.get(1))
    }
}