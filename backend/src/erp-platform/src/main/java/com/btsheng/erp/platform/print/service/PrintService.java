package com.btsheng.erp.platform.print.service;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.platform.print.dto.LabelData;
import com.btsheng.erp.platform.print.dto.PdfA4PrintRequest;
import com.btsheng.erp.platform.print.dto.PrintLogResponse;
import com.btsheng.erp.platform.print.dto.PrintStatisticsBucket;
import com.btsheng.erp.platform.print.dto.ReprintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintResult;
import com.btsheng.erp.platform.print.entity.SysPrintLog;
import com.btsheng.erp.platform.print.mapper.SysPrintLogMapper;
import com.btsheng.erp.platform.printer.entity.SysPrinter;
import com.btsheng.erp.platform.printer.mapper.SysPrinterMapper;
import com.btsheng.erp.platform.print.protocol.LabelProtocol;
import com.btsheng.erp.platform.print.protocol.PdfA4Generator;
import com.btsheng.erp.platform.print.protocol.ProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 双模式打�?Service（V1.3.9 Sprint 12 · Story 12.4�? *
 * <p>5 业务方法�? * <ul>
 * <li>{@link #sendZpl} 模式一 ZPL/TSPL 直连（@Async + 3s Socket 超时�?/li>
 *   <li>{@link #generatePdfA4} 模式�?A4 PDF�?×9=27 标签/页）</li>
 * <li>{@link #replay} 补打（防 reference 递归�?/li>
 *   <li>{@link #listPrintLogs} 历史分页 + 多维过滤</li>
 * <li>{@link #getStatistics} groupBy 聚合</li>
 * </ul>
 * 
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Service
public class PrintService {

    private static final Logger log = LoggerFactory.getLogger(PrintService.class);

    /** Socket connect timeout 3 秒（�?architect R1/R4 决策一�?· IMPL 注意事项 3�?*/
    public static final int SOCKET_CONNECT_TIMEOUT_MS = 3000;
    /** ZPL 发送后等待打印机缓�?200ms */
    public static final int SOCKET_BUFFER_WAIT_MS = 200;
    /** PDF 排版 · 27 标签/页（3×9=50mm×30mm × 9=270mm 高度�?*/
    public static final int PDF_LABELS_PER_PAGE = 27;
    /** PDF 单次最�?30 标签（OpenAPI 强校验） */
    public static final int PDF_MAX_ITEMS = 30;

    private final SysPrintLogMapper printLogMapper;
    private final SysPrinterMapper printerMapper;
    private final ProtocolFactory protocolFactory;
    private final PdfA4Generator pdfA4Generator;

    @Autowired
    public PrintService(SysPrintLogMapper printLogMapper,
                         SysPrinterMapper printerMapper,
                         ProtocolFactory protocolFactory,
                         PdfA4Generator pdfA4Generator) {
        this.printLogMapper = printLogMapper;
        this.printerMapper = printerMapper;
        this.protocolFactory = protocolFactory;
        this.pdfA4Generator = pdfA4Generator;
    }

    // ==================== 模式一 ZPL/TSPL 直连 ====================

    /**
 * TC-12.4.1.1~1.8 模式一 ZPL/TSPL 直连打印
     *
 * <p>@Async 线程池执�?· 不阻�?HTTP 线程
     * <p>流程�?
 * <ol>
     *   <li>预检 sys_printer 状�?协议 �?50201 OFFLINE / 50202 PROTOCOL_UNSUPPORTED</li>
 * <li>ProtocolFactory �?ZplProtocol / TsplProtocol �?字节�?/li>
     *   <li>3s 硬�?connect 超时 · OutputStream.write + flush</li>
 * <li>200ms 缓冲等待 · close · �?sys_print_log status=SUCCESS</li>
     *   <li>失败 �?50203 ZPL_SEND_FAILED · status=FAILED + error_msg</li>
 * </ol>
     */
    @Async("printZplExecutor")
    @AuditLog(module = "SYS_PRINT_LOG", action = "ZPL_DIRECT")
    public void sendZpl(ZplPrintRequest req, Long operatorUserId, String operatorName, Long tenantId) {
        // 同步预检（在主线程内先创�?PENDING log · 然后异步执行�?        // 注：@Async 方法必须有返回值才能让 caller 拿到 printLogId
        // 此处采用 caller 同步创建 PENDING log + 异步 send + callback 更新
    }

    /**
 * 模式一完整流程（同�?wrapper · 实际 sendZpl �?@Async�?     *
     * <p>Caller 流程：预检 �?创建 PENDING log �?@Async sendZpl �?返回 printLogId
     */
    public Result<ZplPrintResult> printZpl(ZplPrintRequest req, Long operatorUserId, String operatorName, Long tenantId) {
        long start = System.currentTimeMillis();
        // 1. 业务校验
            Result<ZplPrintResult> validateResult = validateZplRequest(req);
        if (validateResult != null) return validateResult;

        // 2. 预检 sys_printer
            SysPrinter printer = printerMapper.selectById(req.getPrinterId());
        if (printer == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "打印机不存在");
        }
        if (!SysPrinter.STATUS_ONLINE.equals(printer.getStatus())) {
            return Result.fail(SysPrintLog.ERR_PRINTER_OFFLINE,
                    "PRINTER_OFFLINE: " + printer.getName() + " 当前不可用，请联系管理员");
        }
        // 3. 预检 protocol（注册校验）
            if (!protocolFactory.has(printer.getProtocol())) {
            return Result.fail(SysPrintLog.ERR_PROTOCOL_UNSUPPORTED,
                    "PROTOCOL_UNSUPPORTED: " + printer.getProtocol());
        }

        // 4. 创建 PENDING log
            SysPrintLog logRow = new SysPrintLog();
        logRow.setLogNo(generateLogNo(tenantId));
        logRow.setOperatorUserId(operatorUserId);
        logRow.setOperatorName(operatorName);
        logRow.setPrintedAt(LocalDateTime.now());
        logRow.setCodeType(req.getTemplateCode());
        logRow.setCodeValue(req.getQrContent());
        logRow.setCopies(req.getCount());
        logRow.setPrinterId(printer.getId());
        logRow.setPrinterNameSnapshot(printer.getName());
        logRow.setPrinterIpSnapshot(printer.getIp());
        logRow.setPrintMode(SysPrintLog.MODE_ZPL_DIRECT);
        logRow.setStatus(SysPrintLog.STATUS_PENDING);
        logRow.setRemark(req.getRemark());
        logRow.setCreatedAt(LocalDateTime.now());
        logRow.setTenantId(tenantId);
        printLogMapper.insert(logRow);

        Long logId = logRow.getId();

        // 5. @Async 异步执行
            try {
            asyncSendZpl(logId, req, printer, operatorUserId, operatorName, tenantId);
        } catch (Exception e) {
            // 异步任务投递失�?· �?FAILED
            updateLogFailed(logId, "异步投递失�? " + e.getMessage());
            return Result.fail(SysPrintLog.ERR_ZPL_SEND_FAILED, "ZPL_SEND_FAILED: " + e.getMessage());
        }

        // 6. 立即返回 · printLogId + 字节数预�?
            ZplPrintResult r = new ZplPrintResult();
        r.setPrintLogId(logId);
        r.setLogNo(logRow.getLogNo());
        r.setBytesSent(0);  // 异步完成后回�?
            r.setLatencyMs((int) (System.currentTimeMillis() - start));
        r.setProtocol(printer.getProtocol());
        return Result.ok(r);
    }

    /**
 * @Async 实际发送（独立线程 · 不阻�?HTTP�?     */
    @Async("printZplExecutor")
    public void asyncSendZpl(Long logId, ZplPrintRequest req, SysPrinter printer,
                              Long operatorUserId, String operatorName, Long tenantId) {
        long start = System.currentTimeMillis();
        try {
            // 1. 选协�?+ 渲染字节�?
            LabelProtocol protocol = protocolFactory.get(printer.getProtocol());
            LabelData data = new LabelData();
            data.setTemplateCode(req.getTemplateCode());
            data.setQrContent(req.getQrContent());
            data.setLines(req.getLines());
            data.setColorBarHex(req.getColorBarHex());
            byte[] payload = protocol.render(data, req.getCount());

            // 2. Socket 连接 · 3s 硬性超�?
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(printer.getIp(), printer.getPort()),
                        SOCKET_CONNECT_TIMEOUT_MS);
                socket.getOutputStream().write(payload);
                socket.getOutputStream().flush();
                // 200ms 缓冲
            Thread.sleep(SOCKET_BUFFER_WAIT_MS);
            }

            // 3. �?SUCCESS log
            int latency = (int) (System.currentTimeMillis() - start);
            updateLogSuccess(logId, payload.length, latency);
            log.info("[PrintService] ZPL sendZpl success: logId={} bytes={} latencyMs={}",
                    logId, payload.length, latency);
        } catch (java.net.SocketTimeoutException e) {
            updateLogFailed(logId, "SocketTimeout: " + e.getMessage());
            log.warn("[PrintService] ZPL sendZpl timeout: logId={} ip={} {}", logId, printer.getIp(), e.getMessage());
        } catch (IOException e) {
            updateLogFailed(logId, e.getClass().getSimpleName() + ": " + e.getMessage());
            log.warn("[PrintService] ZPL sendZpl io: logId={} ip={} {}", logId, printer.getIp(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateLogFailed(logId, "Interrupted: " + e.getMessage());
        } catch (Exception e) {
            updateLogFailed(logId, e.getClass().getSimpleName() + ": " + e.getMessage());
            log.error("[PrintService] ZPL sendZpl unknown: logId={}", logId, e);
        }
    }

    private void updateLogSuccess(Long logId, int bytesSent, int latencyMs) {
        SysPrintLog update = new SysPrintLog();
        update.setId(logId);
        update.setStatus(SysPrintLog.STATUS_SUCCESS);
        // bytes/latency 暂不存表（统计维度有限）· 如需可加字段
            printLogMapper.updateById(update);
    }

    private void updateLogFailed(Long logId, String errorMsg) {
        SysPrintLog update = new SysPrintLog();
        update.setId(logId);
        update.setStatus(SysPrintLog.STATUS_FAILED);
        update.setErrorMsg(errorMsg);
        printLogMapper.updateById(update);
    }

    // ==================== 模式�?A4 PDF ====================

    /**
 * TC-12.4.2.1~2.6 模式�?A4 PDF 生成
     */
    @AuditLog(module = "SYS_PRINT_LOG", action = "PDF_BROWSER")
    public Result<Map<String, Object>> printPdfA4(PdfA4PrintRequest req, Long operatorUserId, String operatorName, Long tenantId) {
        // 1. 业务校验
            if (req.getItems() == null || req.getItems().isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "items 至少 1 项");
        }
        if (req.getItems().size() > PDF_MAX_ITEMS) {
            return Result.fail(Result.CODE_PARAM_BOUND,
                    "单次最多 " + PDF_MAX_ITEMS + " 标签/页（A4 排版）");
        }

        // 2. 创建 log（PDF_BROWSER 模式 · printer_id=NULL · name="普通浏览器"�?
            SysPrintLog logRow = new SysPrintLog();
        logRow.setLogNo(generateLogNo(tenantId));
        logRow.setOperatorUserId(operatorUserId);
        logRow.setOperatorName(operatorName);
        logRow.setPrintedAt(LocalDateTime.now());
        logRow.setCodeType(req.getItems().get(0).getTemplateCode());
        logRow.setCodeValue(req.getItems().get(0).getQrContent());
        logRow.setCopies(req.getItems().size());
        logRow.setPrinterId(null);
        logRow.setPrinterNameSnapshot(SysPrintLog.BROWSER_PRINTER_NAME);
        logRow.setPrinterIpSnapshot(null);
        logRow.setPrintMode(SysPrintLog.MODE_PDF_BROWSER);
        logRow.setStatus(SysPrintLog.STATUS_SUCCESS);
        logRow.setRemark(req.getRemark());
        logRow.setCreatedAt(LocalDateTime.now());
        logRow.setTenantId(tenantId);
        printLogMapper.insert(logRow);

        // 3. 生成 PDF
            byte[] pdfBytes = pdfA4Generator.generate(req.getItems(), logRow.getLogNo(), null, "Smart Workshop ERP");

        // 4. 返回（含 logId 用于前端回填�?
            Map<String, Object> data = new HashMap<>();
        data.put("printLogId", logRow.getId());
        data.put("logNo", logRow.getLogNo());
        data.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(pdfBytes));
        data.put("bytes", pdfBytes.length);
        data.put("contentType", "application/pdf");
        data.put("filename", "labels-" + logRow.getLogNo() + ".pdf");
        return Result.ok(data);
    }

    // ==================== 补打 ====================

    /**
 * TC-12.4.3.5/3.6 补打（同模式/换模式）
     *
 * <p>防递归：source.reference_log_id != null �?拒绝 40954
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "SYS_PRINT_LOG", action = "REPLAY")
    public Result<Map<String, Object>> replay(Long sourceId, ReprintRequest req,
                                                Long operatorUserId, String operatorName, Long tenantId) {
        // 1. 查源 log
            SysPrintLog source = printLogMapper.selectById(sourceId);
        if (source == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "源日志不存在");
        }
        // 2. 防补打递归
            if (!SysPrintLog.STATUS_SUCCESS.equals(source.getStatus())) {
            throw new BizException(SysPrintLog.ERR_REPLAY_FORBIDDEN,
                    "PRINT_REPLAY_FORBIDDEN: 仅 SUCCESS 可补打");
        }
        if (source.getReferenceLogId() != null) {
            throw new BizException(SysPrintLog.ERR_REPLAY_FORBIDDEN,
                    "PRINT_REPLAY_FORBIDDEN: 仅 SUCCESS 可补打");
        }

        // 3. 目标模式
            String targetMode = req.getTargetMode() == null || "SAME".equals(req.getTargetMode())
                ? source.getPrintMode() : req.getTargetMode();

        // 4. 创建�?log（reference_log_id = sourceId�?
            SysPrintLog newLog = new SysPrintLog();
        newLog.setLogNo(generateLogNo(tenantId));
        newLog.setOperatorUserId(operatorUserId);
        newLog.setOperatorName(operatorName);
        newLog.setPrintedAt(LocalDateTime.now());
        newLog.setCodeType(source.getCodeType());
        newLog.setCodeValue(source.getCodeValue());
        newLog.setCopies(source.getCopies());
        newLog.setReferenceLogId(sourceId);
        newLog.setPrintMode(targetMode);
        newLog.setStatus(SysPrintLog.STATUS_PENDING);
        newLog.setRemark(req.getRemark());
        newLog.setCreatedAt(LocalDateTime.now());
        newLog.setTenantId(tenantId);

        // 5. �?targetMode 分发
            if (SysPrintLog.MODE_PDF_BROWSER.equals(targetMode)) {
            newLog.setPrinterNameSnapshot(SysPrintLog.BROWSER_PRINTER_NAME);
            printLogMapper.insert(newLog);
            // 同步生成 PDF
            List<PdfA4PrintRequest.LabelItem> items = new ArrayList<>();
            PdfA4PrintRequest.LabelItem item = new PdfA4PrintRequest.LabelItem();
            item.setTemplateCode(source.getCodeType());
            item.setQrContent(source.getCodeValue());
            item.setLines(java.util.Collections.emptyList());
            items.add(item);
            byte[] pdf = pdfA4Generator.generate(items, newLog.getLogNo(), null, "Smart Workshop ERP");
            newLog.setStatus(SysPrintLog.STATUS_SUCCESS);
            printLogMapper.updateById(newLog);
            Map<String, Object> data = new HashMap<>();
            data.put("printLogId", newLog.getId());
            data.put("logNo", newLog.getLogNo());
            data.put("referenceLogId", sourceId);
            data.put("mode", targetMode);
            data.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(pdf));
            return Result.ok(data);
        } else {
            // ZPL 模式
            Long printerId = req.getPrinterId() != null ? req.getPrinterId() : source.getPrinterId();
            if (printerId == null) {
                return Result.fail(Result.CODE_PARAM_MISSING, "ZPL 模式补打需指定 printerId");
            }
            SysPrinter printer = printerMapper.selectById(printerId);
            if (printer == null) {
                return Result.fail(Result.CODE_NOT_FOUND, "打印机不存在");
            }
            newLog.setPrinterId(printer.getId());
            newLog.setPrinterNameSnapshot(printer.getName());
            newLog.setPrinterIpSnapshot(printer.getIp());
            printLogMapper.insert(newLog);
            // 异步发�?
            ZplPrintRequest zplReq = new ZplPrintRequest();
            zplReq.setTemplateCode(source.getCodeType());
            zplReq.setQrContent(source.getCodeValue());
            zplReq.setLines(java.util.Collections.emptyList());
            zplReq.setPrinterId(printerId);
            zplReq.setCount(source.getCopies());
            try {
                asyncSendZpl(newLog.getId(), zplReq, printer, operatorUserId, operatorName, tenantId);
            } catch (Exception e) {
                updateLogFailed(newLog.getId(), "补打异步投递失�? " + e.getMessage());
            }
            Map<String, Object> data = new HashMap<>();
            data.put("printLogId", newLog.getId());
            data.put("logNo", newLog.getLogNo());
            data.put("referenceLogId", sourceId);
            data.put("mode", targetMode);
            return Result.ok(data);
        }
    }

    // ==================== 历史查询 ====================

    /**
 * TC-12.4.3.1/3.2/3.3 多维过滤分页查询
     */
    public Result<PageResponse<PrintLogResponse>> listPrintLogs(String codeType, String mode, String status,
                                                                 Long operatorId, String codeValue,
                                                                 LocalDateTime dateFrom, LocalDateTime dateTo,
                                                                 int page, int size, Long tenantId) {
        long total = printLogMapper.countByFilters(codeType, mode, status, operatorId, codeValue,
                dateFrom, dateTo, tenantId);
        int offset = (Math.max(page, 1) - 1) * size;
        List<SysPrintLog> records = printLogMapper.selectByFilters(codeType, mode, status, operatorId, codeValue,
                dateFrom, dateTo, size, offset, tenantId);
        List<PrintLogResponse> items = new ArrayList<>();
        for (SysPrintLog r : records) items.add(toResponse(r));
        return Result.ok(new PageResponse<>(items, total, page, size));
    }

    /**
 * TC-12.4.3.4 单条详情
     */
    public Result<PrintLogResponse> getPrintLog(Long id, Long tenantId) {
        SysPrintLog r = printLogMapper.selectById(id);
        if (r == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "日志不存在");
        }
        if (tenantId != null && !tenantId.equals(r.getTenantId())) {
            return Result.fail(Result.CODE_FORBIDDEN, "无权访问");
        }
        return Result.ok(toResponse(r));
    }

    // ==================== 统计 ====================

    /**
 * TC-12.4.4.1/4.2 groupBy 聚合
     */
    public Result<List<PrintStatisticsBucket>> getStatistics(String groupBy,
                                                              LocalDateTime dateFrom, LocalDateTime dateTo,
                                                              Long tenantId) {
        // 默认 30 �?
            LocalDateTime end = dateTo != null ? dateTo : LocalDateTime.now();
        LocalDateTime start = dateFrom != null ? dateFrom : end.minusDays(30);

        // �?groupBy + status 聚合
            List<Map<String, Object>> rows = printLogMapper.aggregateByGroupAndStatus(groupBy, start, end, tenantId);

        // 合并�?bucket
            Map<String, PrintStatisticsBucket> bucketMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get("bucket_key"));
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.get("cnt")).longValue();
            PrintStatisticsBucket bucket = bucketMap.computeIfAbsent(key, k -> {
                PrintStatisticsBucket b = new PrintStatisticsBucket();
                b.setKey(k);
                return b;
            });
            if (SysPrintLog.STATUS_SUCCESS.equals(status)) bucket.setSuccessCount(cnt);
            else if (SysPrintLog.STATUS_FAILED.equals(status)) bucket.setFailedCount(cnt);
        }
        List<PrintStatisticsBucket> result = new ArrayList<>(bucketMap.values());
        for (PrintStatisticsBucket b : result) {
            b.setTotalCount((b.getSuccessCount() == null ? 0 : b.getSuccessCount())
                    + (b.getFailedCount() == null ? 0 : b.getFailedCount()));
        }
        return Result.ok(result);
    }

    // ==================== helpers ====================

    /**
 * ZPL 请求校验
     */
    private Result<ZplPrintResult> validateZplRequest(ZplPrintRequest req) {
        if (req == null) return Result.fail(Result.CODE_PARAM_MISSING, "请求体为空");
        if (req.getTemplateCode() == null) return Result.fail(Result.CODE_PARAM_MISSING, "templateCode 必填");
        if (req.getQrContent() == null) return Result.fail(Result.CODE_PARAM_MISSING, "qrContent 必填");
        if (req.getPrinterId() == null) return Result.fail(Result.CODE_PARAM_MISSING, "printerId 必填");
        if (req.getCount() == null || req.getCount() < 1 || req.getCount() > 100) {
            return Result.fail(Result.CODE_PARAM_BOUND, "count 范围 1-100");
        }
        if (req.getLines() != null && req.getLines().size() > 6) {
            return Result.fail(Result.CODE_PARAM_BOUND, "lines 最多 6 行");
        }
        return null;
    }

    /**
 * 生成 log_no · PR-{yyyyMMdd}-{seq:4}
     */
    private String generateLogNo(Long tenantId) {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = printLogMapper.nextLogNoSeq(tenantId, LocalDateTime.now());
        return String.format("PR-%s-%04d", date, seq);
    }

    /**
 * Entity �?DTO
     */
    private PrintLogResponse toResponse(SysPrintLog r) {
        PrintLogResponse resp = new PrintLogResponse();
        resp.setId(r.getId());
        resp.setLogNo(r.getLogNo());
        resp.setOperatorUserId(r.getOperatorUserId());
        resp.setOperatorName(r.getOperatorName());
        resp.setPrintedAt(r.getPrintedAt());
        resp.setCodeType(r.getCodeType());
        resp.setCodeValue(r.getCodeValue());
        resp.setCopies(r.getCopies());
        resp.setPrinterId(r.getPrinterId());
        resp.setPrinterNameSnapshot(r.getPrinterNameSnapshot());
        resp.setPrinterIpSnapshot(r.getPrinterIpSnapshot());
        resp.setPrintMode(r.getPrintMode());
        resp.setStatus(r.getStatus());
        resp.setErrorMsg(r.getErrorMsg());
        resp.setReferenceLogId(r.getReferenceLogId());
        resp.setRemark(r.getRemark());
        resp.setTenantId(r.getTenantId());
        return resp;
    }
}
