package com.btsheng.erp.production.outsource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "委外明细（crm_outsource_item）")
@TableName("crm_outsource_item")
public class CrmOutsourceItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("outsource_no")  private String outsourceNo;
    @TableField("item_seq")      private Integer itemSeq;
    @TableField("material_code") private String materialCode;
    @TableField("material_name") private String materialName;
    @TableField("spec")          private String spec;
    @TableField("qty")           private Integer qty;
    @TableField("unit")          private String unit = "个";
    @TableField("unit_price")    private BigDecimal unitPrice = BigDecimal.ZERO;
    @TableField("total_amount")  private BigDecimal totalAmount = BigDecimal.ZERO;
    @TableField("delivery_date") private LocalDate deliveryDate;
    @TableField("remark")        private String remark;
}
