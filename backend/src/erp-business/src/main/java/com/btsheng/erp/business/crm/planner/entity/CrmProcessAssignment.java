package com.btsheng.erp.business.crm.planner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V2.1 · 报价与订单协同设计 · 工序分配
 */
@Data
@Schema(description = "工序分配（crm_process_assignment）")
@TableName("crm_process_assignment")
public class CrmProcessAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("planning_id") private Long planningId;           // 排产计划ID
    @TableField("sequence") private Integer sequence = 0;        // 工序序号
    @TableField("process_name") private String processName;     // 工序名称
    @TableField("machine_type") private String machineType;   // 设备类型
    @TableField("machine_id") private Long machineId;          // 设备ID
    @TableField("machine_code") private String machineCode;    // 设备编号
    @TableField("operator_user_id") private Long operatorUserId;  // 操作工ID
    @TableField("operator_name") private String operatorName;  // 操作工姓名
    @TableField("planned_start") private LocalDateTime plannedStart;  // 计划开始
    @TableField("planned_end") private LocalDateTime plannedEnd;      // 计划结束
    @TableField("actual_start") private LocalDateTime actualStart;    // 实际开始
    @TableField("actual_end") private LocalDateTime actualEnd;        // 实际结束
    @TableField("is_outsource") private Integer isOutsource = 0;  // 是否委外
    @TableField("outsource_vendor_id") private Long outsourceVendorId;  // 委外供应商ID
    @TableField("remark") private String remark;                      // 备注
}
