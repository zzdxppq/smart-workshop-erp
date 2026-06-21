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
@Schema(description = "工序过站（crm_production_station）")
@TableName("crm_production_station")
public class CrmProductionStation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("transfer_no")        private String transferNo;
    @TableField("workorder_no")       private String workorderNo;
    @TableField("from_step_no")       private Integer fromStepNo;
    @TableField("to_step_no")         private Integer toStepNo;
    @TableField("from_equipment_id")  private Long fromEquipmentId;
    @TableField("to_equipment_id")    private Long toEquipmentId;
    @TableField("transferred_by")     private Long transferredBy;
    @TableField("transferred_at")     private LocalDateTime transferredAt;
    @TableField("remark")             private String remark;
}
