package com.btsheng.erp.production.outsource.incoming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.25 · 委外来料检验项目
 */
@Data
@Schema(description = "来料检验项目（crm_outsource_incoming_item · Story 1.25）")
@TableName("crm_outsource_incoming_item")
public class CrmOutsourceIncomingItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id")  private Long inspectionId;
    @TableField("item_name")      private String itemName;
    @TableField("standard")       private String standard;
    @TableField("measured_value") private String measuredValue;
    @TableField("passed")         private Integer passed = 0;
    @TableField("created_at")     private LocalDateTime createdAt;
}
