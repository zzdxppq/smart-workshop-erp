package com.btsheng.erp.core.scan

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class QrCodeParserTest {
    @Test fun `GD 工单码`() { assertEquals(QrCodeParser.TYPE_WORK_ORDER, QrCodeParser.parse("GD-20260610-0001").type) }
    @Test fun `WL 物料码`() { assertEquals(QrCodeParser.TYPE_MATERIAL, QrCodeParser.parse("WL-STEEL-001").type) }
    @Test fun `LZ 流转码`() { assertEquals(QrCodeParser.TYPE_FLOW, QrCodeParser.parse("LZ-GD001-P01").type) }
    @Test fun `SB 设备码`() { assertEquals(QrCodeParser.TYPE_DEVICE, QrCodeParser.parse("SB-CNC-001").type) }
    @Test fun `WW 委外 + UNKNOWN`() {
        assertEquals(QrCodeParser.TYPE_OUTSOURCE_ORDER, QrCodeParser.parse("WW-20260610-0001").type)
        assertEquals(QrCodeParser.TYPE_OUTSOURCE_ORDER, QrCodeParser.parse("WW20260612-0001").type)
        assertEquals("WW20260612-0001", QrCodeParser.parse("WW-20260612-0001").code)
        assertFalse(QrCodeParser.parse("WN-20260610-0001").ok)
        assertEquals(QrCodeParser.TYPE_UNKNOWN, QrCodeParser.parse("QR-12345").type)
    }
}
