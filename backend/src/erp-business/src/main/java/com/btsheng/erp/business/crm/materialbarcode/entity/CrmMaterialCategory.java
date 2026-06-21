package com.btsheng.erp.business.crm.materialbarcode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.11 · 物料分类（P2 修补 3 · 5 段聚合）
 * 5 分类：MAT-RAW (WL) / MAT-BUY (WJ) / MAT-MFG (ZZ) / MAT-OUT (WW) / MAT-FIN (CP)
 */
@Data
@Schema(description = "物料分类（crm_material_category）")
@TableName("crm_material_category")
public class CrmMaterialCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("category_code") private String categoryCode;   // MAT-RAW / MAT-BUY / MAT-MFG / MAT-OUT / MAT-FIN
            @TableField("category_name") private String categoryName;
    @TableField("prefix")        private String prefix;         // WL / WJ / ZZ / WW / CP
            @TableField("seq_no")        private Integer seqNo = 0;
    @TableField("is_active")     private Integer isActive = 1;
    @TableField("remark")        private String remark;
    @TableField("created_at")    private LocalDateTime createdAt;
    @TableField("updated_at")    private LocalDateTime updatedAt;
}
