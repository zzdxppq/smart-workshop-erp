package com.btsheng.erp.business.crm.engineer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2.1 · 报价与订单协同设计 · BOM子件明细
 */
@Data
@Schema(description = "BOM子件明细（crm_bom_detail）")
@TableName("crm_bom_detail")
public class CrmBomDetailItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("workbench_id") private Long workbenchId;      // 工作台ID
    @TableField("item_type") private String itemType;       // MATERIAL/TOOL/PIN/SOCKET/CONSUMABLE/PACKAGE
    @TableField("sequence") private Integer sequence = 0; // 序号
    @TableField("material_code") private String materialCode;  // 物料编码
    @TableField("material_name") private String materialName;  // 物料名称
    @TableField("spec") private String spec;               // 规格
    @TableField("quantity") private BigDecimal quantity = BigDecimal.ONE;  // 用量
    @TableField("unit") private String unit = "个";       // 单位
    @TableField("source") private String source = "STOCK";  // 来源：STOCK/PURCHASE/OUTSOURCE
    @TableField("remark") private String remark;           // 备注
}
