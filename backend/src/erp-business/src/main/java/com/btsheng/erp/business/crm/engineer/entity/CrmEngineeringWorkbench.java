package com.btsheng.erp.business.crm.engineer.entity;

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
 * V2.1 · 报价与订单协同设计 · 工程转化工作台
 */
@Data
@Schema(description = "工程转化工作台（crm_engineering_workbench）")
@TableName("crm_engineering_workbench")
public class CrmEngineeringWorkbench implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id") private Long orderId;                 // 订单ID
    @TableField("order_item_id") private Long orderItemId;       // 订单明细ID
    @TableField("drawing_no") private String drawingNo;         // 图号
    @TableField("material_no") private String materialNo;       // 料号
    @TableField("status") private String status = "PENDING";    // PENDING/IN_PROGRESS/COMPLETED
    @TableField("process_status") private String processStatus = "PENDING";  // PENDING/IN_PROGRESS/COMPLETED
    @TableField("bom_status") private String bomStatus = "PENDING";        // PENDING/IN_PROGRESS/COMPLETED
    @TableField("engineer_user_id") private Long engineerUserId;  // 负责工程师ID
    @TableField("engineer_name") private String engineerName;    // 负责工程师姓名
    @TableField("process_detail") private String processDetail; // 详细工艺参数JSON
    @TableField("bom_detail") private String bomDetail;        // BOM明细JSON
    @TableField("total_hours") private BigDecimal totalHours = BigDecimal.ZERO;  // 总工时
    @TableField("remark") private String remark;                 // 备注
    @TableField("started_at") private LocalDateTime startedAt;  // 开始时间
    @TableField("completed_at") private LocalDateTime completedAt;  // 完成时间
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
