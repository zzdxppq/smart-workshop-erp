package com.btsheng.erp.business.internal.productroute.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("mdm_product_route")
public class MdmProductRoute implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("product_code") private String productCode;
    @TableField("process_seq") private Integer processSeq;
    @TableField("process_code") private String processCode;
    @TableField("is_outsource") private Boolean isOutsource = false;
}
