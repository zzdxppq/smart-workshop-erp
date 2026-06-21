package com.btsheng.erp.core.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨模块单号生成器（erp-production 等车间执行模块复用）
 *
 * <p>从 erp-business DocNoGenerator 抽取生产/委外相关 prefix，无 CRM Mapper 依赖。
 */
@Component
public class ErpDocNoGenerator {

    private final ConcurrentHashMap<String, AtomicLong> dailySeq = new ConcurrentHashMap<>();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String nextWorkOrderNo() {
        return nextNo("GD");
    }

    public String nextScheduleNo() {
        return nextNo("SCH");
    }

    public String nextProductionScanNo() {
        return nextNo("PS");
    }

    public String nextReportNo() {
        return nextNo("RP");
    }

    public String nextTransferNo() {
        return nextNo("TR");
    }

    public String nextMrpRunNo() {
        return nextNo("MR");
    }

    /** WW-YYYYMMDD-NNNN · PRD 委外单号规范 */
    public String nextOutsourceOrderNo() {
        String today = LocalDate.now().format(dateFmt);
        String key = "WW-" + today;
        AtomicLong seq = dailySeq.computeIfAbsent(key, k -> new AtomicLong(1));
        long n = seq.getAndIncrement();
        return String.format("WW-%s-%04d", today, n);
    }

    public String nextOutsourceStateNo() {
        return nextNo("OS");
    }

    public String nextReworkNo() {
        return nextNo("RW");
    }

    public String nextOutsourceEtaNo() {
        return nextNo("OE");
    }

    public String nextOutsourceInspectionNo() {
        return nextNo("OI");
    }

    public String nextOutsourceQualityNo() {
        return nextNo("OQ");
    }

    /** PROC{yyyyMMdd}-{seq:4} · Story 1.10 工艺编码 */
    public String nextProcessNo() {
        return nextNo("PROC");
    }

    /** SB-{TYPE}-{seq:3} · E5-S5 设备码 */
    public String nextMachineCode(String type) {
        String t = (type == null || type.isBlank()) ? "CNC" : type.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return String.format("SB-%s-%03d", t, dailySeq.computeIfAbsent("SB-" + t, k -> new AtomicLong(1)).getAndIncrement());
    }

    public String nextNo(String prefix) {
        String today = LocalDate.now().format(dateFmt);
        String key = prefix + today;
        AtomicLong seq = dailySeq.computeIfAbsent(key, k -> new AtomicLong(1));
        long n = seq.getAndIncrement();
        return String.format("%s%s-%04d", prefix, today, n);
    }

    /** 测试入口：注入指定 seq 起点 */
    public void resetForTest(String prefix, long startSeq) {
        String today = LocalDate.now().format(dateFmt);
        dailySeq.put(prefix + today, new AtomicLong(startSeq));
    }
}
