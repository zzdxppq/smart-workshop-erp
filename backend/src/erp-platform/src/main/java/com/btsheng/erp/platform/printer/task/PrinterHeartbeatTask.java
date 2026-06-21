package com.btsheng.erp.platform.printer.task;

import com.btsheng.erp.platform.printer.entity.SysPrinter;
import com.btsheng.erp.platform.printer.mapper.SysPrinterMapper;
import com.btsheng.erp.platform.printer.service.SysPrinterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 打印机心跳调度任务（V1.3.9 Sprint 12 · Story 12.2 · AC-12.2.2）
 *
 * <p>60s 周期 · 单线程 · TCP Socket 探活 · 2s 超时
 * <p>fail_count ≥ 2 才标 OFFLINE（防瞬断误标）
 * <p>NORMAL 类型保持 status=UNKNOWN（OS 打印队列无 IP · 不探活）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component
public class PrinterHeartbeatTask {

    private static final Logger log = LoggerFactory.getLogger(PrinterHeartbeatTask.class);

    private final SysPrinterMapper printerMapper;
    private final SysPrinterService printerService;

    public PrinterHeartbeatTask(SysPrinterMapper printerMapper, SysPrinterService printerService) {
        this.printerMapper = printerMapper;
        this.printerService = printerService;
    }

    /**
     * 60s 周期调度（与 architect 评审对齐 · fixedRate=60_000ms）
     * <p>单线程串行执行 · V1.3.9 客户规模 5-10 台 · V1.4 backlog 改线程池
     */
    @Scheduled(fixedRate = 60_000L, initialDelay = 30_000L)
    public void run() {
        try {
            List<SysPrinter> printers = printerMapper.selectAllEnabledLabels();
            if (printers.isEmpty()) {
                return;
            }
            int online = 0, offline = 0, unknown = 0;
            for (SysPrinter p : printers) {
                printerService.probeHeartbeat(p);
                String st = p.getStatus();
                if (SysPrinter.STATUS_ONLINE.equals(st)) online++;
                else if (SysPrinter.STATUS_OFFLINE.equals(st)) offline++;
                else unknown++;
            }
            log.info("[PrinterHeartbeat] scanned={} online={} offline={} unknown={}",
                    printers.size(), online, offline, unknown);
        } catch (Exception e) {
            log.error("[PrinterHeartbeat] 调度失败", e);
        }
    }
}
