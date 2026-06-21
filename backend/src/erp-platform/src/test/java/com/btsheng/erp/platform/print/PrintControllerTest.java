package com.btsheng.erp.platform.print;

import com.btsheng.erp.platform.print.controller.PrintController;
import com.btsheng.erp.platform.print.dto.PdfA4PrintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintRequest;
import com.btsheng.erp.platform.print.entity.SysPrintLog;
import com.btsheng.erp.platform.printer.entity.SysPrinter;
import com.btsheng.erp.platform.print.mapper.SysPrintLogMapper;
import com.btsheng.erp.platform.printer.mapper.SysPrinterMapper;
import com.btsheng.erp.platform.print.protocol.PdfA4Generator;
import com.btsheng.erp.platform.print.protocol.ProtocolFactory;
import com.btsheng.erp.platform.print.protocol.TsplProtocol;
import com.btsheng.erp.platform.print.protocol.ZplProtocol;
import com.btsheng.erp.platform.print.service.PrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PrintController 端到端测例（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.5 端点契约）
 *
 * <p>6 端点 Sanity：HTTP 200/4xx 状态码 + body
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Story 12.4 · PrintController 6 端点契约")
class PrintControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PrintController controller;
    @MockBean private SysPrintLogMapper printLogMapper;
    @MockBean private SysPrinterMapper printerMapper;
    @MockBean private PdfA4Generator pdfA4Generator;

    // ========== TC-12.4.5 端点 1 · POST /labels/zpl ==========
            @Test
    @DisplayName("TC-12.4.5 /labels/zpl 端点 200 OK")
    void TC_12_4_5_endpoint_zpl() throws Exception {
        when(printLogMapper.nextLogNoSeq(anyLong(), any())).thenReturn(1L);
        SysPrinter p = new SysPrinter();
        p.setId(5L);
        p.setName("Zebra-1");
        p.setStatus("ONLINE");
        p.setProtocol("ZPL");
        p.setIp("192.168.1.100");
        p.setPort(9100);
        when(printerMapper.selectById(5L)).thenReturn(p);

        String body = "{\"templateCode\":\"GD\",\"qrContent\":\"GD-001\",\"lines\":[\"L1\"],\"printerId\":5,\"count\":1}";
        mockMvc.perform(post("/print/labels/zpl")
                        .contentType("application/json")
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "操作员")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.printLogId").exists());
    }

    // ========== TC-12.4.5 端点 2 · POST /labels/pdf-a4 ==========
            @Test
    @DisplayName("TC-12.4.5 /labels/pdf-a4 端点 200 OK · X-Print-Log-Id 头（用业务字段透出）")
    void TC_12_4_5_endpoint_pdf_a4() throws Exception {
        when(printLogMapper.nextLogNoSeq(anyLong(), any())).thenReturn(1L);
        when(pdfA4Generator.generate(any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3, 4, 5});

        String body = "{\"items\":[{\"templateCode\":\"GD\",\"qrContent\":\"GD-001\",\"lines\":[\"L1\"]}]}";
        mockMvc.perform(post("/print/labels/pdf-a4")
                        .contentType("application/json")
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "操作员")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.printLogId").exists())
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"));
    }

    // ========== TC-12.4.5 端点 3 · GET /logs ==========
            @Test
    @DisplayName("TC-12.4.5 /logs 端点 200 OK · 分页 + 多维过滤")
    void TC_12_4_5_endpoint_list_logs() throws Exception {
        when(printLogMapper.countByFilters(any(), any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(0L);
        when(printLogMapper.selectByFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyLong()))
                .thenReturn(Arrays.asList());
        mockMvc.perform(get("/print/logs?codeType=GD&page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ========== TC-12.4.5 端点 4 · GET /logs/{id} ==========
            @Test
    @DisplayName("TC-12.4.5 /logs/{id} 端点 200 OK")
    void TC_12_4_5_endpoint_get_log() throws Exception {
        SysPrintLog log = new SysPrintLog();
        log.setId(1L);
        log.setLogNo("PR-20260614-001");
        log.setCodeType("GD");
        log.setCodeValue("GD-001");
        log.setPrintMode("ZPL_DIRECT");
        log.setStatus("SUCCESS");
        log.setTenantId(1L);
        when(printLogMapper.selectById(1L)).thenReturn(log);

        mockMvc.perform(get("/print/logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.logNo").value("PR-20260614-001"));
    }

    // ========== TC-12.4.5 端点 5 · POST /logs/{id}/replay ==========
            @Test
    @DisplayName("TC-12.4.5 /logs/{id}/replay 端点 200 OK")
    void TC_12_4_5_endpoint_replay() throws Exception {
        SysPrintLog source = new SysPrintLog();
        source.setId(1L);
        source.setCodeType("GD");
        source.setCodeValue("GD-001");
        source.setPrintMode("PDF_BROWSER");
        source.setStatus("SUCCESS");
        source.setCopies(1);
        source.setTenantId(1L);
        source.setReferenceLogId(null);
        when(printLogMapper.selectById(1L)).thenReturn(source);
        when(pdfA4Generator.generate(any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        String body = "{\"targetMode\":\"SAME\"}";
        mockMvc.perform(post("/print/logs/1/replay")
                        .contentType("application/json")
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "操作员")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.referenceLogId").value(1));
    }

    // ========== TC-12.4.5 端点 6 · GET /statistics ==========
            @Test
    @DisplayName("TC-12.4.5 /statistics 端点 200 OK · groupBy=month")
    void TC_12_4_5_endpoint_statistics() throws Exception {
        when(printLogMapper.aggregateByGroupAndStatus(eq("month"), any(), any(), anyLong()))
                .thenReturn(Arrays.asList());
        mockMvc.perform(get("/print/statistics?groupBy=month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
