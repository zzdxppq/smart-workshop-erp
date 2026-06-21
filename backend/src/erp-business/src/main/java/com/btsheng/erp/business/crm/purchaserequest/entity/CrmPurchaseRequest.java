package com.btsheng.erp.business.crm.purchaserequest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_purchase_request")
public class CrmPurchaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("pr_no")
    private String prNo;

    @TableField("mrp_run_id")
    private Long mrpRunId;

    @TableField("mrp_shortage_id")
    private Long mrpShortageId;

    @TableField("workorder_no")
    private String workorderNo;

    @TableField("sales_order_no")
    private String salesOrderNo;

    @TableField("material_id")
    private Long materialId;

    @TableField("material_code")
    private String materialCode;

    @TableField("material_name")
    private String materialName;

    @TableField("required_qty")
    private Integer requiredQty;

    @TableField("converted_qty")
    private Integer convertedQty;

    @TableField("required_date")
    private LocalDate requiredDate;

    @TableField("status")
    private String status;

    @TableField("source_type")
    private String sourceType;

    @TableField("remark")
    private String remark;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
