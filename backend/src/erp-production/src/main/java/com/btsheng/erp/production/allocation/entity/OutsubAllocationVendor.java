package com.btsheng.erp.production.allocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "工序-厂商选择（outsub_allocation_vendor · V1.3.7 采购）")
@TableName("outsub_allocation_vendor")
public class OutsubAllocationVendor implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("allocation_id")
    private Long allocationId;
    @TableField("vendor_id")
    private Long vendorId;
    @TableField("unit_price")
    private BigDecimal unitPrice;
    @TableField("delivery_date")
    private LocalDate deliveryDate;
    @TableField("selected_by_user_id")
    private Long selectedByUserId;
    @TableField("selected_at")
    private LocalDateTime selectedAt;
    @TableField("status")
    private String status = "PENDING";
}
