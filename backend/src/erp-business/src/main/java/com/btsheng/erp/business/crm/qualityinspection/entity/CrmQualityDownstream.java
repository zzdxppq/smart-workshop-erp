package com.btsheng.erp.business.crm.qualityinspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crm_quality_downstream")
public class CrmQualityDownstream implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id") private Long inspectionId;
    @TableField("downstream_type") private String downstreamType;
    @TableField("order_no") private String orderNo;
    @TableField("qty") private Integer qty;
    @TableField("status") private String status;
    @TableField("remark") private String remark;
    @TableField("created_by") private Long createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
}
