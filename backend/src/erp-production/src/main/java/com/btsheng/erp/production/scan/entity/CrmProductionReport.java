package com.btsheng.erp.production.scan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "生产报工（crm_production_report）")
@TableName("crm_production_report")
public class CrmProductionReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("report_no")      private String reportNo;
    @TableField("workorder_no")   private String workorderNo;
    @TableField("step_no")        private Integer stepNo;
    @TableField("reported_qty")   private Integer reportedQty = 0;
    @TableField("actual_minutes") private Integer actualMinutes = 0;
    @TableField("is_abnormal")    private Integer isAbnormal = 0;
    @TableField("abnormal_type")  private String abnormalType;
    @TableField("abnormal_note")  private String abnormalNote;
    @TableField("reported_by")    private Long reportedBy;
    @TableField("reported_at")    private LocalDateTime reportedAt;
}
