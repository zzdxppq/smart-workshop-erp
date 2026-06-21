package com.btsheng.erp.business.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("sales_receipt")
public class SalesReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private Long contractId;

    @TableField("receipt_date")
    private LocalDate receiptDate;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("payer")
    private String payer;

    @TableField("remark")
    private String remark;
}
