package com.btsheng.erp.production.performance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_employee_performance_daily")
public class CrmEmployeePerformanceDaily implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("stat_date") private LocalDate statDate;
    @TableField("operator_id") private Long operatorId;
    @TableField("operator_name") private String operatorName;
    @TableField("machine_id") private Long machineId;
    @TableField("machine_code") private String machineCode;
    @TableField("finished_qty") private Integer finishedQty = 0;
    @TableField("qualified_qty") private Integer qualifiedQty = 0;
    @TableField("scrap_qty") private Integer scrapQty = 0;
    @TableField("actual_minutes") private Integer actualMinutes = 0;
    @TableField("std_minutes") private Integer stdMinutes = 0;
    @TableField("utilization_rate") private BigDecimal utilizationRate;
    @TableField("pass_rate") private BigDecimal passRate;
    @TableField("score") private BigDecimal score;
    @TableField("grade") private String grade;
    @TableField("created_at") private LocalDateTime createdAt;
}
