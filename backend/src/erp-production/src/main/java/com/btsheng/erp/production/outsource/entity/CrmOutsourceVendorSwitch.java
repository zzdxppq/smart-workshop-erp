package com.btsheng.erp.production.outsource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crm_outsource_vendor_switch")
public class CrmOutsourceVendorSwitch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("switch_no") private String switchNo;
    @TableField("outsource_id") private Long outsourceId;
    @TableField("outsource_no") private String outsourceNo;
    @TableField("old_supplier_id") private Long oldSupplierId;
    @TableField("old_supplier_name") private String oldSupplierName;
    @TableField("new_supplier_id") private Long newSupplierId;
    @TableField("new_supplier_name") private String newSupplierName;
    @TableField("reason") private String reason;
    @TableField("status") private String status;
    @TableField("prod_confirmed") private Integer prodConfirmed;
    @TableField("purch_confirmed") private Integer purchConfirmed;
    @TableField("prod_confirmed_by") private Long prodConfirmedBy;
    @TableField("purch_confirmed_by") private Long purchConfirmedBy;
    @TableField("prod_confirmed_at") private LocalDateTime prodConfirmedAt;
    @TableField("purch_confirmed_at") private LocalDateTime purchConfirmedAt;
    @TableField("created_by") private Long createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
