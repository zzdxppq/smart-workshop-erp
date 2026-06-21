package com.btsheng.erp.business.crm.quote.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V2.1 · 报价与订单协同设计 · 报价范本工序明细
 */
@Data
@Schema(description = "报价范本工序明细（crm_quote_template_process）")
@TableName("crm_quote_template_process")
public class CrmQuoteTemplateProcess implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("template_id") private Long templateId;          // 范本ID
    @TableField("sequence") private Integer sequence = 0;        // 工序序号
    @TableField("process_code") private String processCode;     // 工序编码
    @TableField("process_name") private String processName;     // 工序名称
    @TableField("machine_type") private String machineType;     // 设备类型
    @TableField("unit_time_minutes") private Integer unitTimeMinutes = 0;  // 单位工时（分钟）
    @TableField("cost_per_hour") private BigDecimal costPerHour = BigDecimal.ZERO;  // 每小时成本
    @TableField("outsource_flag") private Integer outsourceFlag = 0;  // 是否委外
    @TableField("remark") private String remark;                // 备注
}
