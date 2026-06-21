package com.btsheng.erp.production.material.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_material")
public class CrmMaterial implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_code") private String materialCode;
    @TableField("material_name") private String materialName;
    @TableField("process_id") private Long processId;
    @TableField("is_active") private Integer isActive = 1;
    @TableField("updated_at") private LocalDateTime updatedAt;
    @TableField("cost_total") private BigDecimal costTotal;
}
