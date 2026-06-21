package com.btsheng.erp.business.internal.productroute.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("mdm_process")
public class MdmProcess implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("process_code") private String processCode;
    @TableField("process_name") private String processName;
    @TableField("std_time_min") private BigDecimal stdTimeMin;
    @TableField("machine_type") private String machineType;
    @TableField("unit_price") private BigDecimal unitPrice;
}
