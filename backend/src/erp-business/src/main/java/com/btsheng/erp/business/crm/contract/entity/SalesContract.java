package com.btsheng.erp.business.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@TableName("sales_contract")
public class SalesContract implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_no")
    private String contractNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("file_id")
    private Long fileId;

    @TableField("signed_at")
    private LocalDate signedAt;

    @TableField("status")
    private String status;
}
