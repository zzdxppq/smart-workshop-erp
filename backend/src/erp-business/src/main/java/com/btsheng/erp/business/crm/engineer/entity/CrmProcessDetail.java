package com.btsheng.erp.business.crm.engineer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2.1 · 报价与订单协同设计 · 工艺明细
 */
@Data
@Schema(description = "工艺明细（crm_process_detail）")
@TableName("crm_process_detail")
public class CrmProcessDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("workbench_id") private Long workbenchId;      // 工作台ID
    @TableField("sequence") private Integer sequence = 0;    // 工序序号
    @TableField("process_code") private String processCode; // 工序编码
    @TableField("process_name") private String processName; // 工序名称
    @TableField("machine_type") private String machineType; // 设备类型
    @TableField("machine_id") private Long machineId;      // 设备ID
    @TableField("cost_per_hour") private BigDecimal costPerHour;  // 每小时成本
    @TableField("spindle_speed") private Integer spindleSpeed;  // 转速（rpm）
    @TableField("feed_rate") private BigDecimal feedRate;    // 进给（mm/min）
    @TableField("cutting_depth") private BigDecimal cuttingDepth;  // 切削深度（mm）
    @TableField("tool_no") private String toolNo;           // 刀具号
    @TableField("tool_spec") private String toolSpec;        // 刀具规格
    @TableField("fixture") private String fixture;           // 工装夹具
    @TableField("unit_time_minutes") private Integer unitTimeMinutes = 0;  // 单位工时（分钟）
    @TableField("outsource_flag") private Integer outsourceFlag = 0;  // 是否委外
    @TableField("remark") private String remark;             // 备注
}
