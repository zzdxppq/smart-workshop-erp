package com.btsheng.erp.business.crm.materialbarcode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.11 · 物料主数据
 * 5 段成本：material / labor / machine / overhead / outsource
 *
 * V2.1 改造：新增 drawing_no, generated_from_order 关联图号和订单
 */
@Data
@Schema(description = "物料主数据（crm_material）")
@TableName("crm_material")
public class CrmMaterial implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_code")  private String materialCode;
    @TableField("material_name")  private String materialName;
    @TableField("spec")           private String spec;
    @TableField("unit")           private String unit = "个";
    @TableField("category_id")    private Long categoryId;
    @TableField("process_id")     private Long processId;
    @TableField("drawing_no")     private String drawingNo;  // V2.1 关联的图号（DWG-）
    @TableField("generated_from_order") private String generatedFromOrder;  // V2.1 首次生成该料号的销售订单号
    @TableField("cost_material")  private BigDecimal costMaterial = BigDecimal.ZERO;
    @TableField("cost_labor")     private BigDecimal costLabor = BigDecimal.ZERO;
    @TableField("cost_machine")   private BigDecimal costMachine = BigDecimal.ZERO;
    @TableField("cost_overhead")  private BigDecimal costOverhead = BigDecimal.ZERO;
    @TableField("cost_outsource") private BigDecimal costOutsource = BigDecimal.ZERO;
    @TableField("cost_total")     private BigDecimal costTotal = BigDecimal.ZERO;
    @TableField("is_active")      private Integer isActive = 1;
    @TableField("owner_user_id")  private Long ownerUserId;
    @TableField("dept_id")        private Long deptId = 10L;
    @TableField("remark")         private String remark;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
