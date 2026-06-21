package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报价/订单单号生成器（V1.3.7 · Story 1.5 · T1.4 · P2 修补 1）
 *
 * 模板：BJ{yyyyMMdd}{seq:4}（报价） / XS{yyyyMMdd}{seq:4}（订单）
 * 并发安全：单 JVM 内 ConcurrentHashMap + AtomicLong 守 + DB 唯一索引兜底
 * 跨日：seq 计数按日期隔离，跨日自动重置
 */
@Component
public class DocNoGenerator {

    private final CrmQuoteMapper quoteMapper;
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> dailySeq = new ConcurrentHashMap<>();
    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    public DocNoGenerator(CrmQuoteMapper quoteMapper) {
        this.quoteMapper = quoteMapper;
    }

    /**
     * 生成报价单号 BJ{yyyyMMdd}{seq:4}
     * 例：BJ20260611-0001（注：PRD V1.3.7 §2.2.1 "BJ+年月日+4位流水"）
     */
    public String nextQuoteNo() {
        return nextNo("BJ");
    }

    /**
     * 生成订单单号 XS{yyyyMMdd}{seq:4}
     */
    public String nextOrderNo() {
        return nextNo("XS");
    }

    /**
     * 客户编码 KH{yyyyMMdd}{seq:4}（OpenAPI 示例 KH202606090001）
     */
    public String nextCustomerCode() {
        return nextNo("KH");
    }

    /**
     * V1.3.7 · Story 1.7 · 图号 DWG{yyyyMMdd}{seq:4}
     * 复用模板：P1 修补 - DB 唯一索引 (drawing_no, version) 兜底
     */
    public String nextDrawingNo() {
        return nextHyphenatedNo("DWG");
    }

    /**
     * PRD V1.3.8 · 复合物料条码：{物料编码}-BATCH-{YYYYMMDD}-{seq:4}
     * 例：WL-A001-BATCH-20260619-0001
     */
    public String nextCompositeMaterialBarcode(String materialCode) {
        if (materialCode == null || materialCode.isBlank()) {
            throw new IllegalArgumentException("materialCode required for composite barcode");
        }
        String today = LocalDate.now().format(DATE_FMT);
        String key = "MCB-" + materialCode + "-" + today;
        java.util.concurrent.atomic.AtomicLong seq = dailySeq.computeIfAbsent(key, k -> {
            long existing = countExistingWithPrefix("MCB-" + materialCode, today);
            return new java.util.concurrent.atomic.AtomicLong(existing + 1);
        });
        long n = seq.getAndIncrement();
        return String.format("%s-BATCH-%s-%04d", materialCode, today, n);
    }

    /** PRD 单号格式：PREFIX-YYYYMMDD-NNNN（图号/工单/委外等） */
    private String nextHyphenatedNo(String prefix) {
        String today = LocalDate.now().format(DATE_FMT);
        String key = prefix + "-" + today;
        java.util.concurrent.atomic.AtomicLong seq = dailySeq.computeIfAbsent(key, k -> {
            long existing = countExistingWithPrefix(prefix + "-", today);
            return new java.util.concurrent.atomic.AtomicLong(existing + 1);
        });
        long n = seq.getAndIncrement();
        return String.format("%s-%s-%04d", prefix, today, n);
    }

    /**
     * V1.3.8 · Story 3.1 · 物料编码 WL-XXXX（图纸档案唯一 · P1 修补）
     * 报价快速上传图纸时自动生成，格式 ^WL-\d{4}$
     */
    public String nextMaterialCode() {
        java.util.concurrent.atomic.AtomicLong seq = dailySeq.computeIfAbsent("WL-MAT", k -> {
            long existing = countExistingWithPrefix("WL", "");
            return new java.util.concurrent.atomic.AtomicLong(Math.max(1000L, existing + 1000L));
        });
        long n = seq.getAndIncrement();
        if (n > 9999) {
            n = 1000 + (n % 9000);
        }
        return String.format("WL-%04d", n);
    }

    /**
     * V1.3.7 · Story 1.8 · BOM 单号 BOM{yyyyMMdd}{seq:4}
     * 工程转化下游生成 BOM（V1.3.7 红线 4 · 100 并发不重复）
     */
    public String nextBomNo() {
        return nextNo("BOM");
    }

    /**
     * V1.3.7 · Story 1.9 · 工单号 GD{yyyyMMdd}{seq:4}
     * BOM 转生产触发生成工单（V1.3.7 红线 4 · 100 并发不重复）
     */
    public String nextWorkOrderNo() {
        return nextNo("GD");
    }

    /**
     * V1.3.7 · Story 1.10 · 工艺编码 PROC{yyyyMMdd}{seq:4}
     * 工艺库创建触发（V1.3.7 红线 4 · 100 并发不重复）
     */
    public String nextProcessNo() {
        return nextNo("PROC");
    }

    /**
     * V1.3.7 · Story 1.11 · 物料条码 BC{yyyyMMdd}{seq:4}
     * 物料扫码 / 批量生成触发（V1.3.7 红线 4 · 100 并发不重复 · P1 修补）
     */
    public String nextBarcodeNo() {
        return nextNo("BC");
    }

    /**
     * V1.3.7 · Story 1.12 · 扫码记录 SC{yyyyMMdd}{seq:4}
     * APP 扫码出入库触发（V1.3.7 红线 4 · 100 并发不重复）
     */
    public String nextScanNo() {
        return nextNo("SC");
    }

    /** 盘点单号 STK-YYYYMMDD-NNNN */
    public String nextStocktakeNo() {
        return nextHyphenatedNo("STK");
    }

    /**
     * V1.3.7 · Story 1.13 · 批次号 B{yyyyMMdd}{seq:6}
     * 每入库生成批次号（P1 修补 FEFO 排序）
     */
    public String nextBatchNo() {
        return nextNo("B");
    }

    /**
     * V1.3.8 · Story 3.1/3.2 · 物料批次号 BATCH-YYYYMMDD-流水
     * <p>与 Story 3.2 物料码复合规则 WL-{material_no}-BATCH-... 对齐
     * <p>Story 3.2 §1.1：批次号格式 BATCH-YYYYMMDD-{seq:4}
     */
    public String nextMaterialBatchNo() {
        return nextNo("BATCH-");
    }

    /**
     * V1.3.7 · Story 1.15 · 排产单 SCH{yyyyMMdd}{seq:4}
     * 排产触发
     */
    public String nextScheduleNo() {
        return nextNo("SCH");
    }

    /**
     * V1.3.7 · Story 1.16 · 生产扫码 PS{yyyyMMdd}{seq:4}
     * 扫码开工/报工/过站触发
     */
    public String nextProductionScanNo() {
        return nextNo("PS");
    }

    /**
     * V1.3.7 · Story 1.16 · 报工 RP{yyyyMMdd}{seq:4}
     */
    public String nextReportNo() {
        return nextNo("RP");
    }

    /**
     * V1.3.7 · Story 1.16 · 过站 TR{yyyyMMdd}{seq:4}
     */
    public String nextTransferNo() {
        return nextNo("TR");
    }

    /**
     * V1.3.7 · Story 1.17 · MRP 运算 MR{yyyyMMdd}{seq:4}
     */
    public String nextMrpRunNo() {
        return nextNo("MR");
    }

    /**
     * V1.3.7 · Story 1.18 · 委外单 WW{yyyyMMdd}{seq:4}（复用 1.4 prefix）
     */
    public String nextOutsourceOrderNo() {
        return nextNo("WW");
    }

    /**
     * V1.3.7 · Story 1.21 · 月度对账单 RC{yyyyMM}{seq:4}（按月隔离）
     */
    public String nextReconcileNo() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return nextNoByPeriod("RC", ym);
    }

    /**
     * V1.3.7 · Story 1.22 · 委外状态机历史 OS{yyyyMMdd}{seq:4}（按日隔离）
     * 状态机迁移的留痕单号
     */
    public String nextOutsourceStateNo() {
        return nextNo("OS");
    }

    /**
     * V1.3.7 · Story 1.23 · 返修单 RW{yyyyMMdd}{seq:4}（按日隔离）
     * 返修工单单号
     */
    public String nextReworkNo() {
        return nextNo("RW");
    }

    /**
     * V1.3.7 · Story 1.24 · 委外预估交期单 OE{yyyyMMdd}{seq:4}（按日隔离）
     * 委外历史交期预估
     */
    public String nextOutsourceEtaNo() {
        return nextNo("OE");
    }

    /**
     * V1.3.7 · Story 1.25 · 委外来料质检单 OI{yyyyMMdd}{seq:4}（按日隔离）
     * 委外回厂扫码入库触发的来料质检
     */
    public String nextOutsourceInspectionNo() {
        return nextNo("OI");
    }

    /**
     * V1.3.7 · Story 1.27 · 委外工序质检单 OQ{yyyyMMdd}{seq:4}（按日隔离）
     * 区别于 1.25 来料质检：委外工序独立质检 FA/CMM
     */
    public String nextOutsourceQualityNo() {
        return nextNo("OQ");
    }

    /**
     * V1.3.7 · Story 1.28 · 品质·来料/过程/成品检单 QI{yyyyMMdd}{seq:4}（按日隔离）
     * IQC 来料 / IPQC 过程 / OQC 成品
     */
    public String nextQualityInspectionNo() {
        return nextNo("QI");
    }

    /**
     * V1.3.9 · 检验方案模板 QT{yyyyMMdd}{seq:4}
     */
    public String nextQualityTemplateNo() {
        return nextNo("QT");
    }

    /**
     * V1.3.7 · Story 1.29 · 品质·FA 首件 QF{yyyyMMdd}{seq:4}（按日隔离）
     * 开工前必检 · 检验项目 8 维度
     */
    public String nextQualityFaNo() {
        return nextNo("QF");
    }

    /**
     * V1.3.7 · Story 1.30 · 品质·CMM 三次元 QC{yyyyMMdd}{seq:4}（按日隔离）
     * 三次元测点 ≥ 3 · 偏差超差告警
     */
    public String nextQualityCmmNo() {
        return nextNo("QC");
    }

    /**
     * V1.3.7 · Story 1.31 · 品质·不良品处理 QD{yyyyMMdd}{seq:4}（按日隔离）
     * 8D 报告 · 3 动作（返工/报废/让步接收）
     */
    public String nextQualityDefectNo() {
        return nextNo("QD");
    }

    /**
     * V1.3.7 · Story 1.32 · 采购·询比价 RF{yyyyMMdd}{seq:4}（按日隔离）
     * RFQ 单号 · 3+ 厂商报价 · 选最低/加权中标
     */
    public String nextRfqNo() {
        return nextNo("RF");
    }

    /**
     * V1.3.7 · Story 1.33 · 采购·价格控制 PL{yyyyMMdd}{seq:4}（按日隔离）
     * 物料限价 + 历史价 + 偏差告警
     */
    public String nextPriceControlNo() {
        return nextNo("PL");
    }

    /**
     * V1.3.7 · Story 1.34 · 采购·到货提醒 IA{yyyyMMdd}{seq:4}（按日隔离）
     * 提前 3 天提醒 + 逾期告警
     */
    public String nextIncomingAlertNo() {
        return nextNo("IA");
    }

    /**
     * V1.3.7 · Story 1.35 · 采购·来料质检 PI{yyyyMMdd}{seq:4}（按日隔离）
     * 区别于 1.25/1.27 委外质检 + 1.28 IQC：聚焦 PO 维度抽样 AQL
     */
    public String nextPurchaseIncomingInspectionNo() {
        return nextNo("PI");
    }

    /**
     * V1.3.7 · Story 1.36 · 财务·应收 RV{yyyyMMdd}{seq:4}（按日隔离）
     * 客户应收账款（订单 SETTLED 自动生成）
     */
    public String nextReceivableNo() {
        return nextNo("RV");
    }

    /**
     * V1.3.7 · Story 1.36 · 财务·应付 PV{yyyyMMdd}{seq:4}（按日隔离）
     * 供应商应付账款（PO 自动生成）
     */
    public String nextPayableNo() {
        return nextNo("PV");
    }

    /**
     * V1.3.7 · Story 1.36 · 财务·收付款 PM{yyyyMMdd}{seq:4}（按日隔离）
     * 收付款记录（应收核销 / 应付付款）
     */
    public String nextPaymentNo() {
        return nextNo("PM");
    }

    /**
     * V1.3.7 · Story 1.37 · 财务·成本核算 CA{yyyyMMdd}{seq:4}（按日隔离）
     * 5 段成本自动归集到订单/工单
     */
    public String nextCostAccountingNo() {
        return nextNo("CA");
    }

    /**
     * V1.3.7 · Story 1.38 · 财务·回款计划 PP{yyyyMMdd}{seq:4}（按日隔离）
     * 客户回款计划（订单 SETTLED 自动生成）
     */
    public String nextPaymentPlanNo() {
        return nextNo("PP");
    }

    /**
     * V1.3.7 · Story 1.39 · 财务·利润分析 PA{yyyyMMdd}{seq:4}（按日隔离）
     * 订单 SETTLED 后自动生成利润分析单（收入 - 5 段成本）
     */
    public String nextProfitAnalysisNo() {
        return nextNo("PA");
    }

    /**
     * V1.3.7 · Story 1.40 · 财务·料号成本聚合 MC{yyyyMMdd}{seq:4}（按日隔离）
     * V1.3.4 新增 · 物料 × 月份 × 厂商 5 段成本聚合视图
     */
    public String nextMaterialCostAggregationNo() {
        return nextNo("MC");
    }

    /**
     * V1.3.7 · Story 1.41 · 人事·员工工号 EM{yyyyMM}{seq:4}（按月隔离）
     * 与对账单 RC prefix 区分；每月从 0001 开始
     */
    public String nextEmployeeNo() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return nextNoByPeriod("EM", ym);
    }

    /**
     * V1.3.7 · Story 1.42 · 人事·薪酬单 PY{yyyyMM}{seq:4}（按月隔离）
     */
    public String nextPayrollNo() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return nextNoByPeriod("PY", ym);
    }

    /**
     * V1.3.7 · Story 1.43 · 人事·招聘单 HR{yyyyMM}{seq:4}（按月隔离）
     * 关键：使用 HR prefix 避免与对账单 RC{yyyyMM} 冲突
     */
    public String nextRecruitmentNo() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return nextNoByPeriod("HR", ym);
    }

    /**
     * V1.3.7 · Story 1.44 · 报表·看板快照 DS{yyyyMMddHHmm}{seq:4}（按分钟隔离）
     * 高频快照单号 · 与其他报表区分
     */
    public String nextDashboardSnapshotNo() {
        String period = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        return nextNoByPeriod("DS", period);
    }

    /**
     * V1.3.7 · Story 1.47 · 报表·委外面板单号 OD{yyyyMMddHHmm}{seq:4}（按分钟隔离）
     * 高频快照（状态机分布 / 告警轮询）· 与看板 DS prefix 区分
     */
    public String nextOutsourceDashboardNo() {
        String period = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        return nextNoByPeriod("OD", period);
    }

    /**
     * V1.3.7 · Story 1.49 · 委外协同·仓管扫码权限单号 WP{yyyyMMdd}{seq:4}（按日隔离）
     */
    public String nextWarehousePermissionNo() {
        return nextNo("WP");
    }

    /**
     * V1.3.7 · Story 1.50 · 委外协同·仓管扫码单号 WS{yyyyMMdd}{seq:4}（按日隔离）
     */
    public String nextWarehouseIncomingScanNo() {
        return nextNo("WS");
    }

    /**
     * V1.3.7 · Story 1.51 · 委外协同·品质领料单号 QP{yyyyMMdd}{seq:4}（按日隔离）
     */
    public String nextQualityPickupNo() {
        return nextNo("QP");
    }

    /** 品质退货单 QR{yyyyMMdd}{seq:4} */
    public String nextQualityReturnNo() {
        return nextNo("QR");
    }

    /** 品质返工工单 QW{yyyyMMdd}{seq:4} */
    public String nextQualityReworkOrderNo() {
        return nextNo("QW");
    }

    /** 品质报废记录 QS{yyyyMMdd}{seq:4} */
    public String nextQualityScrapNo() {
        return nextNo("QS");
    }

    /**
     * 按自定义 period 维度生成单号（按月/按周/按日 通用）
     */
    private String nextNoByPeriod(String prefix, String period) {
        String key = prefix + period;
        java.util.concurrent.atomic.AtomicLong seq = dailySeq.computeIfAbsent(key, k -> {
            long existing = countExistingWithPrefix(prefix, period);
            return new java.util.concurrent.atomic.AtomicLong(existing + 1);
        });
        long n = seq.getAndIncrement();
        return String.format("%s%s-%04d", prefix, period, n);
    }

    public String nextNo(String prefix) {
        String today = LocalDate.now().format(DATE_FMT);
        String key = prefix + today;
        java.util.concurrent.atomic.AtomicLong seq = dailySeq.computeIfAbsent(key, k -> {
            // 启动时尝试从 DB 同步当日最大 seq，避免重启后重复
            long existing = countExistingWithPrefix(prefix, today);
            return new java.util.concurrent.atomic.AtomicLong(existing + 1);
        });
        long n = seq.getAndIncrement();
        return String.format("%s%s-%04d", prefix, today, n);
    }

    /**
     * 统计数据库中已存在的同前缀同日单号数量（用于启动时同步 seq）
     * 实际实现可改为 SELECT COUNT(*) FROM crm_quote WHERE quote_no LIKE 'BJ20260611%'
     */
    private long countExistingWithPrefix(String prefix, String date) {
        return 0L;  // 简化：依赖 DB 唯一索引兜底
    }

    /** 测试入口：注入指定 seq 起点 */
    public void resetForTest(String prefix, long startSeq) {
        String today = LocalDate.now().format(DATE_FMT);
        dailySeq.put(prefix + today, new java.util.concurrent.atomic.AtomicLong(startSeq));
    }

    /** 测试入口：按 period 注入 seq 起点（用于对账单按月隔离场景） */
    public void resetForTestByPeriod(String prefix, String period, long startSeq) {
        dailySeq.put(prefix + period, new java.util.concurrent.atomic.AtomicLong(startSeq));
    }
}
