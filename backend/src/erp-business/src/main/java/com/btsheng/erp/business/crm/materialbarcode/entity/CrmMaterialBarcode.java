package com.btsheng.erp.business.crm.materialbarcode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.11 · AC-4.1.1 · 物料条码主表
 *
 * 唯一索引：barcode_no
 * 3 P1 修补：物料编码唯一 / 5 类码 prefix 严格 / 批量生成 100 并发不重复
 * 3 P2 修补：QR Code 二维码 / 条码打印 PDF / 物料分类 5 段聚合
 */
@Data
@Schema(description = "物料条码主表（crm_material_barcode）")
@TableName("crm_material_barcode")
public class CrmMaterialBarcode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("barcode_no")    private String barcodeNo;         // BC{yyyyMMdd}{seq:4}
            @TableField("material_code") private String materialCode;      // WL-XXXX / WJ-XXXX / ZZ-XXXX / WW-XXXX / CP-XXXX
    @TableField("spec")          private String spec;
    @TableField("payload")       private String payload;           // AES-256-GCM 加密
            @TableField("process_id")    private Long processId;
    @TableField("cost_breakdown") private String costBreakdown;    // JSON 5 段成本
            @TableField("batch_no")      private String batchNo;           // 批次号
    @TableField("qty")           private Integer qty = 1;
    @TableField("qr_code_url")   private String qrCodeUrl;         // 二维码 base64
            @TableField("status")        private String status = "ACTIVE"; // ACTIVE / USED / DISCARDED
    @TableField("generated_by")  private Long generatedBy;
    @TableField("generated_at")  private LocalDateTime generatedAt;
    @TableField("updated_at")    private LocalDateTime updatedAt;
}
