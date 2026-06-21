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
@TableName("sales_payment_plan")
public class SalesPaymentPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private Long contractId;

    @TableField("period_no")
    private Integer periodNo;

    @TableField("plan_date")
    private LocalDate planDate;

    @TableField("plan_amount")
    private BigDecimal planAmount;

    @TableField("actual_amount")
    private BigDecimal actualAmount;

    @TableField("status")
    private String status;
}
