package com.btsheng.erp.business.finance.cost.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.37 · 财务·5 段成本明细（crm_cost_segment · FR-9-2）
 */
@Data
@Schema(description = "5 段成本明细（Story 1.37 FR-9-2）")
@TableName("crm_cost_segment")
public class CrmCostSegment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("cost_id")       private Long costId;
    @TableField("segment_code")  private String segmentCode;
    @TableField("segment_name")  private String segmentName;
    @TableField("amount")        private BigDecimal amount;
    @TableField("source")        private String source;
    @TableField("remark")        private String remark;
}
