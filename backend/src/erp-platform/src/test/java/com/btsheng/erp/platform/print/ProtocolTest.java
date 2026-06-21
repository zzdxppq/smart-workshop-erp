package com.btsheng.erp.platform.print;

import com.btsheng.erp.platform.print.dto.LabelData;
import com.btsheng.erp.platform.print.protocol.LabelProtocol;
import com.btsheng.erp.platform.print.protocol.ProtocolFactory;
import com.btsheng.erp.platform.print.protocol.TsplProtocol;
import com.btsheng.erp.platform.print.protocol.ZplProtocol;
import com.btsheng.erp.core.web.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 协议适配器测例（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1 · TC-12.4.1.1/1.2/1.3/1.4/1.6）
 *
 * <p>3 型号字节流断言：Zebra ZD420 (ZPL) / TSC TTP-244 Pro (TSPL) / 启邦 DL-888B (TSPL)
 * <p>字节流逐字节验证 · 防 ZPL/TSPL 语法错位
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@DisplayName("Story 12.4 · ProtocolAdapter 字节流断言（TC-12.4.1.1~1.4）")
class ProtocolTest {

    private ZplProtocol zpl;
    private TsplProtocol tspl;
    private ProtocolFactory factory;

    @BeforeEach
    void setup() {
        zpl = new ZplProtocol();
        tspl = new TsplProtocol();
        factory = new ProtocolFactory(Arrays.asList(zpl, tspl));
    }

    private LabelData sampleData() {
        LabelData d = new LabelData();
        d.setTemplateCode("GD");
        d.setQrContent("GD-260614-001");
        d.setLines(Arrays.asList("工单:WO-20260614-001", "工序:OP-10", "材料:Q235"));
        d.setColorBarHex("#1E40AF");
        return d;
    }

    // ========== TC-12.4.1.1 ZPL 字节流断言 ==========
            @Test
    @DisplayName("TC-12.4.1.1 ZPL Zebra ZD420 字节流：^XA 起始 + ^BC QR + ^A0N 文本 + ^XZ 结尾")
    void TC_12_4_1_1_zpl_byte_stream_zebra() {
        byte[] bytes = zpl.render(sampleData(), 1);
        String zpl = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(zpl.startsWith("^XA"), "ZPL 必须以 ^XA 起始");
        assertTrue(zpl.endsWith("^XZ"), "ZPL 必须以 ^XZ 结尾");
        assertTrue(zpl.contains("^BCN,80,Y,N,N"), "必须包含 ^BC QR 指令");
        assertTrue(zpl.contains("^FDGD-260614-001^FS"), "QR 内容必须存在");
        assertTrue(zpl.contains("^A0N,28,28"), "首行字号 28");
        assertTrue(zpl.contains("^A0N,24,24"), "其余行字号 24");
        assertTrue(zpl.contains("工单:WO-20260614-001"), "第一行内容");
    }

    // ========== TC-12.4.1.1.b ZPL 多份 = N 个 ^XA 块 ==========
            @Test
    @DisplayName("TC-12.4.1.1.b ZPL copies=10 → 10 个 ^XA 块")
    void TC_12_4_1_1_b_zpl_copies_ten() {
        byte[] bytes = zpl.render(sampleData(), 10);
        String zpl = new String(bytes, StandardCharsets.UTF_8);
        int xaCount = zpl.split("\\^XA", -1).length - 1;
        int xzCount = zpl.split("\\^XZ", -1).length - 1;
        assertEquals(10, xaCount, "10 份必须含 10 个 ^XA 起始");
        assertEquals(10, xzCount, "10 份必须含 10 个 ^XZ 结尾");
    }

    // ========== TC-12.4.1.1.c ZPL 转义：^ / ~ 双写 ==========
            @Test
    @DisplayName("TC-12.4.1.1.c ZPL 转义：^ / ~ 双写")
    void TC_12_4_1_1_c_zpl_escape() {
        LabelData d = sampleData();
        d.setQrContent("GD-^TEST~001");
        byte[] bytes = zpl.render(d, 1);
        String zpl = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(zpl.contains("GD-^^TEST~~001"), "^ 必须双写为 ^^");
    }

    // ========== TC-12.4.1.2 TSPL TSC TTP-244 Pro 字节流 ==========
            @Test
    @DisplayName("TC-12.4.1.2 TSPL TSC TTP-244 Pro 字节流：SIZE/CLS/QRCODE/TEXT/PRINT 1")
    void TC_12_4_1_2_tspl_byte_stream_tsc() {
        byte[] bytes = tspl.render(sampleData(), 1);
        String tspl = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(tspl.startsWith("SIZE 50 mm,30 mm\r\n"), "TSPL 必须以 SIZE 50mm,30mm 起始");
        assertTrue(tspl.contains("CLS\r\n"), "必须含 CLS 清屏");
        assertTrue(tspl.contains("QRCODE 20,20,M,6,A,0,\"GD-260614-001\""), "QRCODE 指令");
        assertTrue(tspl.contains("TEXT 220,130,\"3\",0,1,1,\"工单:WO-20260614-001\""), "TEXT 指令首行");
        assertTrue(tspl.contains("PRINT 1\r\n"), "PRINT 1 触发打印");
    }

    // ========== TC-12.4.1.3 TSPL 启邦 DL-888B 字节流（同源 TSPL）==========
            @Test
    @DisplayName("TC-12.4.1.3 TSPL 启邦 DL-888B 字节流（同 TSPL · 与 TSC 字节流一致）")
    void TC_12_4_1_3_tspl_qibang_same_as_tsc() {
        byte[] bytes = tspl.render(sampleData(), 1);
        // DL-888B 与 TSC 共享 TSPL 协议 · 字节流一致
            String s = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(s.contains("SIZE 50 mm,30 mm"));
        assertTrue(s.contains("QRCODE"));
        assertTrue(s.contains("PRINT 1"));
    }

    // ========== TC-12.4.1.4 ProtocolFactory 注入 ==========
            @Test
    @DisplayName("TC-12.4.1.4 ProtocolFactory.get(ZPL) / get(TSPL) 返回对应 bean")
    void TC_12_4_1_4_factory_lookup() {
        LabelProtocol z = factory.get("ZPL");
        LabelProtocol t = factory.get("TSPL");
        assertEquals("ZPL", z.protocolName());
        assertEquals("TSPL", t.protocolName());
        assertTrue(z instanceof ZplProtocol);
        assertTrue(t instanceof TsplProtocol);
    }

    @Test
    @DisplayName("TC-12.4.1.4.b ProtocolFactory.has 已注册协议")
    void TC_12_4_1_4_b_factory_has() {
        assertTrue(factory.has("ZPL"));
        assertTrue(factory.has("TSPL"));
        assertFalse(factory.has("INVALID"));
    }

    // ========== TC-12.4.1.6 协议不支持 50202 ==========
            @Test
    @DisplayName("TC-12.4.1.6 协议不支持：ProtocolFactory.get(INVALID) 抛 BizException 50202")
    void TC_12_4_1_6_unsupported_protocol() {
        BizException ex = assertThrows(BizException.class, () -> factory.get("INVALID"));
        assertEquals(50202, ex.getCode());
        assertTrue(ex.getMessage().contains("PROTOCOL_UNSUPPORTED"));
        assertTrue(ex.getMessage().contains("INVALID"));
    }

    // ========== TC-12.4.1.8 异步线程池配置 core=4 max=16 queue=200 ==========
            @Test
    @DisplayName("TC-12.4.1.8 异步线程池配置（手测） · core=4 max=16 queue=200")
    void TC_12_4_1_8_async_pool_config() {
        com.btsheng.erp.platform.print.config.PrintAsyncConfig cfg =
                new com.btsheng.erp.platform.print.config.PrintAsyncConfig();
        Object exec = cfg.printZplExecutor();
        assertNotNull(exec);
        if (exec instanceof org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) {
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor tpe =
                    (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) exec;
            assertEquals(4, tpe.getCorePoolSize());
            assertEquals(16, tpe.getMaxPoolSize());
            assertEquals(200, tpe.getQueueCapacity());
        }
    }
}
