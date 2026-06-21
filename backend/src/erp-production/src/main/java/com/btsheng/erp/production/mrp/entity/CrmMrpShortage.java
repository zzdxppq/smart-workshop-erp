package com.btsheng.erp.production.mrp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "MRP 缺料清单（crm_mrp_shortage）")
@TableName("crm_mrp_shortage")
public class CrmMrpShortage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("run_id")             private Long runId;
    @TableField("material_code")      private String materialCode;
    @TableField("shortage_qty")       private Integer shortageQty;
    @TableField("required_date")      private LocalDate requiredDate;
    @TableField("priority")           private Integer priority = 5;
    @TableField("source_workorders")  private String sourceWorkorders;
}
