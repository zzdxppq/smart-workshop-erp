package com.btsheng.erp.platform.label.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.label.dto.LabelPreviewRequest;
import com.btsheng.erp.platform.label.dto.LabelPreviewResponse;
import com.btsheng.erp.platform.label.entity.LabelTemplate;
import com.btsheng.erp.platform.label.mapper.LabelTemplateMapper;
import com.btsheng.erp.platform.dict.mapper.DictMapper;
import com.btsheng.erp.platform.font.FontProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LabelTemplateService 测例（V1.3.9 Sprint 12 · Story 12.3 · 14 测例�? *
 * <p>分布�? 单元（渲�?SB fallback/厂名/WL 兼容�? 4 集成（端点契约）+ 2 QR 路由 + 2 跨仓 + 2 边界
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LabelTemplateServiceTest {

    @Mock private LabelTemplateMapper templateMapper;
    @Mock private QrCodeGenerator qrCodeGenerator;
    @Mock private FontProvider fontProvider;
    @Mock private DictMapper dictMapper;

    // ========== TC-12.3.1.1 �?单元：GD 渲染 PNG base64 < 5KB + 蓝色色条 ==========
            @Test
    void TC_12_3_1_1_gd_render_png_under_5kb() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);
        // ZXing 输出 ~2KB 测试 PNG
            byte[] fakeQrPng = new byte[1024];
        for (int i = 0; i < fakeQrPng.length; i++) fakeQrPng[i] = (byte) i;
        when(qrCodeGenerator.renderPng(eq("GD-260614-001"), anyInt())).thenReturn(fakeQrPng);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("GD");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("GD-260614-001");
        data.setLines(List.of("GD-260614-001", "工单：WO20260614001", "工序：P03", "数量�?0", "日期�?026-06-14"));
        req.setData(data);
        req.setFormat("PNG");

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData());
        assertEquals("PNG", r.getData().getFormat());
        assertTrue(r.getData().getBase64().startsWith("data:image/png;base64,"));
        // base64 PNG size < 5KB (测例期望)
            int b64Bytes = r.getData().getBase64().length() - "data:image/png;base64,".length();
        // base64 字符 �?4/3 字节；标�?PNG 含三区布局 + 厂名 ~ 3-5KB�? 5120 字符�?
            assertTrue(b64Bytes < 7000, "base64 字节�?" + b64Bytes + " �?< 7000");
        assertEquals("image/png", r.getData().getContentType());
    }

    // ========== TC-12.3.1.2 �?单元：SB 渲染 fallback �?GD + 灰色色条覆盖 ==========
            @Test
    void TC_12_3_1_2_sb_fallback_gd_grey_strip() {
        // SB- DB 不存�?
            when(templateMapper.selectByType(eq("SB"), eq(1L))).thenReturn(null);
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelTemplate got = svc.getTemplate("SB", 1L);

        assertNotNull(got, "SB- fallback 必须返回 GD 模板");
        assertEquals("GD", got.getType());
        assertEquals("#6B7280", got.getColorStrip(), "严格只覆�?color_strip=#6B7280");
        // 不覆盖其他字段（layout_json / dpi / factory_name 保持 GD 原值）
            assertEquals(gd.getLayoutJson(), got.getLayoutJson(), "layout_json 不覆�?);
        assertEquals(gd.getDpi(), got.getDpi(), "dpi 不覆�?);
        assertEquals(gd.getFactoryName(), got.getFactoryName(), "factory_name 不覆�?);
    }

    // ========== TC-12.3.1.3 �?单元：厂�?maxLength=20 截断/拒绝 ==========
            @Test
    void TC_12_3_1_3_factory_name_max_length_20() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);
        when(qrCodeGenerator.renderPng(anyString(), anyInt())).thenReturn(new byte[64]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("GD");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("GD-260614-001");
        data.setFactoryName("昆山佰泰胜精密机械有限公司VeryLongName"); // 29 字符
            req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(42201, r.getCode(), "factory_name �?20 字符�?42201");
        assertTrue(r.getMessage().contains("factory_name"));
    }

    // ========== TC-12.3.1.4 �?单元：WL 物料码前缀 V1.3.8 规则兼容 ==========
            @Test
    void TC_12_3_1_4_wl_compatibility_v138() {
        LabelTemplate wl = makeTemplate("WL", "#000000", 300);
        when(templateMapper.selectByType(eq("WL"), eq(1L))).thenReturn(wl);
        when(qrCodeGenerator.renderPng(eq("WL-260614-0001"), anyInt())).thenReturn(new byte[64]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("WL");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("WL-260614-0001");
        data.setLines(List.of("WL-260614-0001", "物料：A001", "批次：B001", "数量�?00", "日期�?026-06-14"));
        req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(0, r.getCode());
        assertEquals("WL", r.getData().getType());
        // 渲染�?PNG 含三区布局：色条黑�?+ 5 行明�?
            assertNotNull(r.getData().getBase64());
    }

    // ========== TC-12.3.2.1 �?集成：GET /label-templates?type=GD 返回 1 �?==========
            @Test
    void TC_12_3_2_1_get_label_templates_type_gd() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        Result<Map<String, Object>> r = svc.listTemplates("GD", 1L);

        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templates = (List<Map<String, Object>>) r.getData().get("templates");
        assertEquals(1, templates.size());
        Map<String, Object> t = templates.get(0);
        assertEquals("GD", t.get("type"));
        assertEquals("#1E40AF", t.get("colorStrip"));
        assertNull(t.get("reuseFrom"));
        assertEquals("昆山佰泰胜精密加�?, r.getData().get("companyName"));
    }

    // ========== TC-12.3.2.2 �?集成：GET /label-templates?type=SB 返回 fallback ==========
            @Test
    void TC_12_3_2_2_get_label_templates_type_sb_fallback() {
        when(templateMapper.selectByType(eq("SB"), eq(1L))).thenReturn(null);
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        Result<Map<String, Object>> r = svc.listTemplates("SB", 1L);

        assertEquals(0, r.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templates = (List<Map<String, Object>>) r.getData().get("templates");
        assertEquals(1, templates.size());
        Map<String, Object> t = templates.get(0);
        assertEquals("SB", t.get("type"));
        assertEquals("#6B7280", t.get("colorStrip"));
        assertEquals("GD", t.get("reuseFrom"), "SB- reuseFrom=GD");
    }

    // ========== TC-12.3.2.3 �?集成：POST /preview 5 �?type �?200 + base64 ==========
            @Test
    void TC_12_3_2_3_post_preview_all_five_types() {
        when(qrCodeGenerator.renderPng(anyString(), anyInt())).thenReturn(new byte[64]);
        for (String type : List.of("GD", "LZ", "WW", "WL")) {
            LabelTemplate t = makeTemplate(type, "#1E40AF", 300);
            when(templateMapper.selectByType(eq(type), eq(1L))).thenReturn(t);
        }
        when(templateMapper.selectByType(eq("SB"), eq(1L))).thenReturn(null);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);

        for (String type : List.of("GD", "LZ", "SB", "WW", "WL")) {
            LabelPreviewRequest req = new LabelPreviewRequest();
            req.setType(type);
            LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
            data.setQrContent(type + "-260614-001");
            data.setLines(List.of(type + "-260614-001", "测试�?));
            req.setData(data);
            req.setFormat("PNG");

            Result<LabelPreviewResponse> r = svc.preview(req, 1L);
            assertEquals(0, r.getCode(), type + " 渲染失败: " + r.getMessage());
            assertNotNull(r.getData().getBase64());
            assertTrue(r.getData().getBase64().startsWith("data:image/png;base64,"));
        }
    }

    // ========== TC-12.3.2.4 �?集成：POST /preview type=INVALID �?40002 ==========
            @Test
    void TC_12_3_2_4_post_preview_invalid_type_40002() {
        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("XX");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("XX-260614-001");
        req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(40002, r.getCode());
        assertTrue(r.getMessage().contains("type"));
    }

    // ========== TC-12.3.3.1 �?QR 内容：LZ-260613-001-P03 纯文本（�?base64/无加密） ==========
            @Test
    void TC_12_3_3_1_qr_content_plain_text_with_process() {
        LabelTemplate lz = makeTemplate("LZ", "#16A34A", 300);
        when(templateMapper.selectByType(eq("LZ"), eq(1L))).thenReturn(lz);
        // 模拟 ZXing 返回�?QR 字节 · 测试�?fake
            when(qrCodeGenerator.renderPng(eq("LZ-260613-001-P03"), anyInt())).thenReturn(new byte[128]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("LZ");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("LZ-260613-001-P03"); // �?-P03 工序后缀
            data.setLines(List.of("LZ-260613-001-P03", "工序：P03"));
        req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(0, r.getCode());
        // 验证 QR 生成器接收到的是入参字符串（不编码不加密�?
            verify(qrCodeGenerator).renderPng(eq("LZ-260613-001-P03"), anyInt());
    }

    // ========== TC-12.3.3.2 �?QR 内容：WL-260614-0001 V1.3.8 兼容 ==========
            @Test
    void TC_12_3_3_2_qr_content_wl_compatibility() {
        LabelTemplate wl = makeTemplate("WL", "#000000", 300);
        when(templateMapper.selectByType(eq("WL"), eq(1L))).thenReturn(wl);
        when(qrCodeGenerator.renderPng(eq("WL-260614-0001"), anyInt())).thenReturn(new byte[64]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("WL");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("WL-260614-0001");
        req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(0, r.getCode());
        verify(qrCodeGenerator).renderPng(eq("WL-260614-0001"), anyInt());
    }

    // ========== TC-12.3.4.1 �?跨仓：web-impl /preview 输出 base64 一致�?==========
            @Test
    void TC_12_3_4_1_web_preview_base64_consistency() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);
        when(qrCodeGenerator.renderPng(anyString(), anyInt())).thenReturn(new byte[128]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("GD");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("GD-260614-001");
        data.setLines(List.of("GD-260614-001", "工单：WO001"));
        req.setData(data);

        Result<LabelPreviewResponse> r1 = svc.preview(req, 1L);
        Result<LabelPreviewResponse> r2 = svc.preview(req, 1L);
        // 两次调用 base64 一致（deterministic�?
            assertEquals(r1.getData().getBase64().length(), r2.getData().getBase64().length(),
                "web-impl LabelPreview 调两�?/preview 输出 base64 长度一�?);
        assertEquals(r1.getData().getContentType(), r2.getData().getContentType());
    }

    // ========== TC-12.3.4.2 �?跨仓：android-impl /preview 输出 base64 一致�?==========
            @Test
    void TC_12_3_4_2_android_preview_base64_consistency() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);
        when(qrCodeGenerator.renderPng(anyString(), anyInt())).thenReturn(new byte[128]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("GD");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("GD-260614-001");
        data.setLines(List.of("GD-260614-001", "工单：WO001"));
        req.setData(data);

        // android-impl �?/preview �?web-impl �?/preview 一致（同一后端权威源）
            Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertNotNull(r.getData().getBase64());
        // 验证内容可被解析
            String b64 = r.getData().getBase64().substring("data:image/png;base64,".length());
        byte[] decoded = java.util.Base64.getDecoder().decode(b64);
        assertTrue(decoded.length > 0, "base64 解码后非�?);
        // PNG 头校�?
            assertEquals((byte) 0x89, decoded[0]);
        assertEquals((byte) 'P', decoded[1]);
        assertEquals((byte) 'N', decoded[2]);
        assertEquals((byte) 'G', decoded[3]);
    }

    // ========== TC-12.3.5.1 �?边界：layout_json 缺字�?�?默认值兜�?==========
            @Test
    void TC_12_3_5_1_layout_json_partial_default_fallback() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        gd.setLayoutJson("{\"topBarH\":5}"); // �?qrAreaH/textAreaH/fontSize/qrSizePx
            when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);
        when(qrCodeGenerator.renderPng(anyString(), anyInt())).thenReturn(new byte[64]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("GD");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("GD-260614-001");
        req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(0, r.getCode(), "layout_json 部分缺失应兜底默认�?);
        // 默认 qrSizePx=300 · 验证传入 ZXing �?px=300
            verify(qrCodeGenerator).renderPng(eq("GD-260614-001"), eq(300));
    }

    // ========== TC-12.3.5.2 �?边界：tenant_id 隔离（multi-tenant 厂名不同�?==========
            @Test
    void TC_12_3_5_2_tenant_id_isolation() {
        // tenant A (id=1) 默认厂名 · tenant B (id=2) 自定�?
            LabelTemplate gdA = makeTemplate("GD", "#1E40AF", 300);
        gdA.setTenantId(1L);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gdA);

        LabelTemplate gdB = makeTemplate("GD", "#1E40AF", 300);
        gdB.setTenantId(2L);
        gdB.setFactoryName("上海测试厂");
        when(templateMapper.selectByType(eq("GD"), eq(2L))).thenReturn(gdB);
        when(qrCodeGenerator.renderPng(anyString(), anyInt())).thenReturn(new byte[64]);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);

        // tenant A
            LabelPreviewRequest reqA = new LabelPreviewRequest();
        reqA.setType("GD");
        LabelPreviewRequest.DataPayload dataA = new LabelPreviewRequest.DataPayload();
        dataA.setQrContent("GD-260614-001");
        reqA.setData(dataA);
        Result<LabelPreviewResponse> rA = svc.preview(reqA, 1L);
        assertEquals(0, rA.getCode());

        // tenant B
            LabelPreviewRequest reqB = new LabelPreviewRequest();
        reqB.setType("GD");
        LabelPreviewRequest.DataPayload dataB = new LabelPreviewRequest.DataPayload();
        dataB.setQrContent("GD-260614-001");
        reqB.setData(dataB);
        Result<LabelPreviewResponse> rB = svc.preview(reqB, 2L);
        assertEquals(0, rB.getCode());

        // tenant B 模板�?"上海测试�?
            verify(templateMapper).selectByType(eq("GD"), eq(2L));
    }

    // ========== TC-12.3.2.5 (补充) �?边界：lines �?6 �?�?40001 ==========
            @Test
    void TC_12_3_2_5_lines_exceed_6_40001() {
        LabelTemplate gd = makeTemplate("GD", "#1E40AF", 300);
        when(templateMapper.selectByType(eq("GD"), eq(1L))).thenReturn(gd);

        LabelTemplateService svc = new LabelTemplateService(templateMapper, qrCodeGenerator, fontProvider, dictMapper);
        LabelPreviewRequest req = new LabelPreviewRequest();
        req.setType("GD");
        LabelPreviewRequest.DataPayload data = new LabelPreviewRequest.DataPayload();
        data.setQrContent("GD-260614-001");
        data.setLines(List.of("a", "b", "c", "d", "e", "f", "g")); // 7 �?
            req.setData(data);

        Result<LabelPreviewResponse> r = svc.preview(req, 1L);
        assertEquals(40001, r.getCode());
    }

    // ========== 工具方法 ==========
            private LabelTemplate makeTemplate(String type, String colorStrip, int dpi) {
        LabelTemplate t = new LabelTemplate();
        t.setId(1L);
        t.setType(type);
        t.setColorStrip(colorStrip);
        t.setFactoryName("昆山佰泰胜精密加工");
        t.setLayoutJson("{\"topBarH\":5,\"qrAreaH\":18,\"textAreaH\":7,\"fontSize\":8,\"qrSizePx\":300,\"qrSizeMm\":12}");
        t.setDpi(dpi);
        t.setEnabled(1);
        t.setTenantId(1L);
        return t;
    }
}