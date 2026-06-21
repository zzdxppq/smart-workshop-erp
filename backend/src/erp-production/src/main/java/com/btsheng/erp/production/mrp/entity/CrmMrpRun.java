package com.btsheng.erp.production.mrp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "MRP 运算记录（crm_mrp_run）")
@TableName("crm_mrp_run")
public class CrmMrpRun implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("run_no")                    private String runNo;
    @TableField("run_type")                  private String runType = "FULL";
    @TableField("date_range_start")          private LocalDate dateRangeStart;
    @TableField("date_range_end")            private LocalDate dateRangeEnd;
    @TableField("warehouse_ids")             private String warehouseIds;
    @TableField("status")                    private String status = "RUNNING";
    @TableField("started_at")                private LocalDateTime startedAt;
    @TableField("completed_at")              private LocalDateTime completedAt;
    @TableField("total_shortage")            private Integer totalShortage = 0;
    @TableField("total_purchase_suggestion") private Integer totalPurchaseSuggestion = 0;
    @TableField("triggered_by")              private Long triggeredBy;
    @TableField("remark")                    private String remark;
}
