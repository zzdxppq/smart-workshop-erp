package com.btsheng.erp.platform.printer.service;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.platform.printer.entity.SysPrinter;
import com.btsheng.erp.platform.printer.mapper.SysPrinterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打印�?Service（V1.3.9 Sprint 12 · Story 12.2 · AC-12.2.1/12.2.2�? *
 * <p>5 �?CRUD 端点 + 1 �?/test + 1 �?/available �?6 端点
 * <p>心跳 fail_count �?2 才标 OFFLINE（防瞬断误标�? *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Service
public class SysPrinterService {

    private static final Logger log = LoggerFactory.getLogger(SysPrinterService.class);

    /** TCP 探活超时 2s（与 architect 评审对齐 · 避免长卡阻塞调度�?*/
    public static final int PROBE_TIMEOUT_MS = 2000;
    /** 心跳容差 · 连续失败 2 次才�?OFFLINE */
    public static final int FAIL_COUNT_THRESHOLD = 2;

    private final SysPrinterMapper printerMapper;
    private final com.btsheng.erp.platform.print.mapper.SysPrintLogMapper printLogMapper;

    @Autowired
    public SysPrinterService(SysPrinterMapper printerMapper,
                              com.btsheng.erp.platform.print.mapper.SysPrintLogMapper printLogMapper) {
        this.printerMapper = printerMapper;
        this.printLogMapper = printLogMapper;
    }

    /**
     * TC-12.2.1.1 - 多维过滤分页查询
     */
    public Result<PageResponse<SysPrinter>> listPrinters(String type, String status, Integer enabled,
                                                         long pageNum, long pageSize, Long tenantId) {
        long total = printerMapper.countByFilters(type, status, enabled, tenantId);
        // 简单分页：拉所有过滤后�?pageNum/pageSize 切片（V1.3.9 客户规模 5-10 台）
            List<SysPrinter> all = printerMapper.selectByFilters(type, status, enabled, tenantId);
        long from = Math.max(0, (pageNum - 1) * pageSize);
        long to = Math.min(all.size(), from + pageSize);
        List<SysPrinter> page = all.subList((int) from, (int) to);
        return Result.ok(new PageResponse<>(page, total, pageNum, pageSize));
    }

    /**
     * TC-12.2.1.1 - 创建打印�?     */
    @AuditLog(module = "printer", action = "CREATE")
    public Result<SysPrinter> createPrinter(SysPrinter printer, Long operatorUserId, Long tenantId) {
        // 业务校验：LABEL 必填 ip
            Result<SysPrinter> validate = validateBusinessRules(printer, null);
        if (validate != null) return validate;

        // 业务校验：name 唯一
            if (printerMapper.countByName(printer.getName()) > 0) {
            return Result.fail(Result.CODE_CONFLICT, "打印机名称已存在");
        }

        // 默认�?
            if (printer.getPort() == null) printer.setPort(9100);
        if (printer.getEnabled() == null) printer.setEnabled(1);
        if (printer.getModelSuggestion() == null) printer.setModelSuggestion(SysPrinter.MODEL_OTHER);
        if (printer.getStatus() == null) printer.setStatus(SysPrinter.STATUS_UNKNOWN);
        if (printer.getFailCount() == null) printer.setFailCount(0);

        // NORMAL 类型：protocol 固定 PDF_BROWSER，ip 可空
            if (SysPrinter.TYPE_NORMAL.equals(printer.getType())) {
            printer.setProtocol(SysPrinter.PROTOCOL_PDF_BROWSER);
        }

        printer.setCreatedBy(operatorUserId);
        printer.setUpdatedBy(operatorUserId);
        printer.setTenantId(tenantId);
        printer.setCreatedAt(LocalDateTime.now());
        printer.setUpdatedAt(LocalDateTime.now());
        printerMapper.insert(printer);
        return Result.ok(printer);
    }

    /**
     * TC-12.2.1.3 - 更新打印�?     */
    @AuditLog(module = "printer", action = "UPDATE")
    public Result<SysPrinter> updatePrinter(Long id, SysPrinter printer, Long operatorUserId) {
        SysPrinter existing = printerMapper.selectById(id);
        if (existing == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "打印机不存在");
        }

        // 业务校验
            Result<SysPrinter> validate = validateBusinessRules(printer, id);
        if (validate != null) return validate;

        // 业务校验：name 唯一（排除自己）
            if (printer.getName() != null && !printer.getName().equals(existing.getName())) {
            if (printerMapper.countByNameExcludingId(printer.getName(), id) > 0) {
                return Result.fail(Result.CODE_CONFLICT, "打印机名称已存在");
            }
            existing.setName(printer.getName());
        }

        // 允许修改的字�?
            if (printer.getType() != null) existing.setType(printer.getType());
        if (printer.getIp() != null) existing.setIp(printer.getIp());
        if (printer.getPort() != null) existing.setPort(printer.getPort());
        if (printer.getProtocol() != null) existing.setProtocol(printer.getProtocol());
        if (printer.getModelSuggestion() != null) existing.setModelSuggestion(printer.getModelSuggestion());
        if (printer.getEnabled() != null) existing.setEnabled(printer.getEnabled());

        // NORMAL 类型强制 PDF_BROWSER
            if (SysPrinter.TYPE_NORMAL.equals(existing.getType())) {
            existing.setProtocol(SysPrinter.PROTOCOL_PDF_BROWSER);
        }

        existing.setUpdatedBy(operatorUserId);
        existing.setUpdatedAt(LocalDateTime.now());
        printerMapper.updateById(existing);
        return Result.ok(existing);
    }

    /**
     * TC-12.2.1.5/1.6 - 删除打印机（有引用则 409 提示 enabled=0�?     */
    @AuditLog(module = "printer", action = "DELETE")
    public Result<Void> deletePrinter(Long id) {
        SysPrinter existing = printerMapper.selectById(id);
        if (existing == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "打印机不存在");
        }
        // 12.4 引入 sys_print_log �?· 引用计数检查（FK 已建�?
            int refCount = countPrintLogReferences(id, existing.getTenantId());
        if (refCount > 0) {
            return Result.fail(Result.CODE_CONFLICT_IN_USE,
                    "打印机已�?" + refCount + " 条打印日志引�?· 请改�?enabled=0");
        }
        printerMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * TC-12.2.1.5/1.6 - 删除打印机（带引用检�?· 12.4 引入 sys_print_log 后切换到此方法）
     */
    @AuditLog(module = "printer", action = "DELETE")
    public Result<Void> deletePrinterWithRefCheck(Long id, int refCount) {
        SysPrinter existing = printerMapper.selectById(id);
        if (existing == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "打印机不存在");
        }
        if (refCount > 0) {
            return Result.fail(Result.CODE_CONFLICT_IN_USE, "打印机已被使�?· 请改�?enabled=0");
        }
        printerMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * TC-12.2.2.1/2.2/2.3/2.4 - 手动测试打印机连接（2s 超时�?     */
    public Result<Map<String, Object>> testPrinter(Long id) {
        SysPrinter p = printerMapper.selectById(id);
        if (p == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "打印机不存在");
        }
        if (SysPrinter.TYPE_NORMAL.equals(p.getType())) {
            // NORMAL 类型无需 IP 探活
            Map<String, Object> ok = new HashMap<>();
            ok.put("status", SysPrinter.STATUS_UNKNOWN);
            ok.put("latencyMs", 0);
            ok.put("protocolDetected", "UNKNOWN");
            ok.put("hint", "NORMAL 类型使用 OS 打印队列 · 无需 IP 探活");
            return Result.ok(ok);
        }
        if (p.getIp() == null || p.getIp().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", SysPrinter.STATUS_OFFLINE);
            err.put("error", "ip 未配置");
            err.put("hint", "请检�?IP/端口配置");
            return Result.ok(err);
        }

        long start = System.currentTimeMillis();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(p.getIp(), p.getPort()), PROBE_TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> ok = new HashMap<>();
            ok.put("status", SysPrinter.STATUS_ONLINE);
            ok.put("latencyMs", latency);
            ok.put("protocolDetected", p.getProtocol());
            return Result.ok(ok);
        } catch (IOException e) {
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> err = new HashMap<>();
            err.put("status", SysPrinter.STATUS_OFFLINE);
            err.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            err.put("hint", "请检�?IP/端口/网线");
            err.put("latencyMs", latency);
            return Result.ok(err);
        }
    }

    /**
     * TC-12.2.3.1/3.2/3.3 - 查询可用同类型打印机
     *
     * <p>返回 enabled=1 的同类型打印�?· 0 台时 printers=null · �?QA 契约一�?     */
    public Result<Map<String, Object>> getAvailablePrinters(String type, Long tenantId) {
        List<SysPrinter> list = printerMapper.selectAvailableByType(type, tenantId);
        Map<String, Object> data = new HashMap<>();
        if (list.isEmpty()) {
            data.put("printers", null);
            data.put("count", 0);
        } else {
            data.put("printers", list);
            data.put("count", list.size());
        }
        return Result.ok(data);
    }

    /**
     * TC-12.2.2.1/2.2/2.3/2.4 - 心跳探活（单�?· �?@Scheduled 调用�?     *
     * <p>TCP Socket 探活 · 2s 超时 · fail_count++ · 连续 2 次失败标 OFFLINE
     * <p>成功�?fail_count=0 · status=ONLINE · lastHeartbeatAt=NOW()
     */
    public void probeHeartbeat(SysPrinter p) {
        if (!SysPrinter.TYPE_LABEL.equals(p.getType()) || p.getEnabled() == null || p.getEnabled() == 0) {
            // NORMAL 类型不探�?· 保持 status=UNKNOWN
            return;
        }
        if (p.getIp() == null || p.getIp().isEmpty()) {
            return;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(p.getIp(), p.getPort()), PROBE_TIMEOUT_MS);
            // 成功
            p.setStatus(SysPrinter.STATUS_ONLINE);
            p.setFailCount(0);
            p.setLastHeartbeatAt(LocalDateTime.now());
            printerMapper.updateHeartbeat(p.getId(), SysPrinter.STATUS_ONLINE, 0, LocalDateTime.now());
        } catch (IOException e) {
            // 失败
            int newFail = (p.getFailCount() == null ? 0 : p.getFailCount()) + 1;
            String newStatus = newFail >= FAIL_COUNT_THRESHOLD
                    ? SysPrinter.STATUS_OFFLINE
                    : p.getStatus(); // 保持不变（容差）
            p.setFailCount(newFail);
            p.setStatus(newStatus);
            printerMapper.updateHeartbeat(p.getId(), newStatus, newFail, p.getLastHeartbeatAt());
            log.debug("[PrinterHeartbeat] printer id={} fail_count={} status={} err={}",
                    p.getId(), newFail, newStatus, e.getMessage());
        }
    }

    /**
     * 业务规则校验：LABEL 必填 ip，port 范围 1-65535，ip 格式
     */
    private Result<SysPrinter> validateBusinessRules(SysPrinter p, Long excludeId) {
        if (p.getName() == null || p.getName().isEmpty() || p.getName().length() > 50) {
            return Result.fail(Result.CODE_PARAM_MISSING, "name 必填 · 1-50 字符");
        }
        if (p.getType() == null
                || (!SysPrinter.TYPE_NORMAL.equals(p.getType()) && !SysPrinter.TYPE_LABEL.equals(p.getType()))) {
            return Result.fail(Result.CODE_PARAM_MISSING, "type 必填 · NORMAL �?LABEL");
        }
        if (SysPrinter.TYPE_LABEL.equals(p.getType())) {
            if (p.getIp() == null || p.getIp().isEmpty()) {
                return Result.fail(42201, "LABEL 类型必填 ip");
            }
            if (!isValidIpv4(p.getIp())) {
                return Result.fail(42201, "字段校验失败 · ip 格式无效");
            }
        }
        if (p.getPort() != null && (p.getPort() < 1 || p.getPort() > 65535)) {
            return Result.fail(Result.CODE_PARAM_BOUND, "port 范围 1-65535");
        }
        return null;
    }

    /**
     * 12.4 引入 sys_print_log �?· 引用计数检�?     */
    private int countPrintLogReferences(Long printerId, Long tenantId) {
        if (printLogMapper == null) return 0;  // 12.2 阶段 fallback
            try {
            long c = printLogMapper.countByPrinterId(printerId, tenantId);
            return (int) c;
        } catch (Exception e) {
            log.warn("[SysPrinterService] countPrintLogReferences failed: {}", e.getMessage());
            return 0;
        }
    }

    private static boolean isValidIpv4(String ip) {
        if (ip == null) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        try {
            for (String s : parts) {
                int v = Integer.parseInt(s);
                if (v < 0 || v > 255) return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
