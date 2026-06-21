package com.btsheng.erp.business.crm.materialdetail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V1.3.8 · Story 2.1 · 料号详情页聚合 DTO（7 Tab）
 *
 * <p>聚合来源：1.7 图纸 + 1.10 工艺库 + 1.33 价格 + 1.40 料号成本 + 1.45 看板 + 1.48 价格面板
 *
 * <p>7 Tab：
 * <ol>
 *   <li>基本信息 base</li>
 *   <li>工艺路线 process</li>
 *   <li>图纸 drawing</li>
 *   <li>价格 price</li>
 *   <li>材料成本 cost</li>
 *   <li>工时成本 labor</li>
 *   <li>外协成本 outsource</li>
 * </ol>
 *
 * <p>Redis 缓存策略（architect review 2.1 §2.2）：key=mat:detail:{id}，5min TTL。
 * 本期 IMPL：DTO 字段完整定义，@Cacheable 实装留 Sprint 7 集成阶段。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "料号详情页聚合 DTO（Story 2.1 · 7 Tab）")
public class MaterialDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Tab 1 基本信息 */
    private BaseInfo base;

    /** Tab 2 工艺路线 */
    private ProcessInfo process;

    /** Tab 3 图纸 */
    private DrawingInfo drawing;

    /** Tab 4 价格 */
    private PriceInfo price;

    /** Tab 5 材料成本 */
    private CostInfo cost;

    /** Tab 6 工时成本 */
    private LaborInfo labor;

    /** Tab 7 外协成本 */
    private OutsourceInfo outsource;

    @Data
    public static class BaseInfo implements Serializable {
        private Long materialId;
        private String materialNo;
        private String name;
        private String spec;
        private String unit;
        private String category;
        private String defaultWarehouse;
    }

    @Data
    public static class ProcessInfo implements Serializable {
        private List<ProcessRoute> routes;

        @Data
        public static class ProcessRoute implements Serializable {
            private Integer stepSeq;
            private String processNo;
            private String workcenter;
            private BigDecimal stdMinutes;
            private String equipment;
        }
    }

    @Data
    public static class DrawingInfo implements Serializable {
        private String dwgNo;
        private String version;
        private String status;
        private String pdfUrl;
        private Boolean isLatest;
    }

    @Data
    public static class PriceInfo implements Serializable {
        private BigDecimal currentPrice;
        private BigDecimal avg30d;
        private BigDecimal min30d;
        private BigDecimal max30d;
        private List<TrendPoint> trendPoints;

        @Data
        public static class TrendPoint implements Serializable {
            private LocalDateTime date;
            private BigDecimal price;
        }
    }

    @Data
    public static class CostInfo implements Serializable {
        private BigDecimal materialCost;
        private BigDecimal scrapRate;
        private BigDecimal effectiveCost;
    }

    @Data
    public static class LaborInfo implements Serializable {
        private BigDecimal laborMinutes;
        private BigDecimal hourlyRate;
        private BigDecimal laborCost;
    }

    @Data
    public static class OutsourceInfo implements Serializable {
        private BigDecimal outsourceCost;
        private String supplier;
        private Integer leadDays;
    }
}