package com.btsheng.erp.business.crm.purchaseinspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * V1.3.7 · Story 1.35 · 采购·来料质检检验项（crm_purchase_incoming_item · FR-8-4）
 */
@Data
@Schema(description = "采购来料质检检验项（Story 1.35 FR-8-4）")
@TableName("crm_purchase_incoming_item")
public class CrmPurchaseIncomingItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id") private Long inspectionId;
    @TableField("seq_no")        private Integer seqNo;
    @TableField("check_item")    private String checkItem;
    @TableField("standard")      private String standard;
    @TableField("sample_qty")    private Integer sampleQty;
    @TableField("pass_qty")      private Integer passQty;
    @TableField("fail_qty")      private Integer failQty;
    @TableField("is_critical")   private Integer isCritical;
    @TableField("result")        private String result = "PENDING";
    @TableField("remark")        private String remark;
}
