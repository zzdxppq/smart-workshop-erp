package com.btsheng.erp.platform.sysparam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** V1.3.7 Story 1.3 · AC-1.3.3 · sys_global_threshold · 双轨（Nacos 优先 + DB 回退） */
@Data
@Schema(description = "金额阈值全局（sys_global_threshold）")
@TableName("sys_global_threshold")
public class GlobalThreshold implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("biz_type")
    @Schema(description = "业务类型：QUOTE/ORDER/PURCHASE/PAYMENT", example = "QUOTE")
    private String bizType;

    @TableField("role_code")
    @Schema(description = "角色编码：salesperson/dept_manager/gm/buyer/finance/finance_director", example = "dept_manager")
    private String roleCode;

    @TableField("threshold")
    @Schema(description = "金额阈值（NULL=无限额）", example = "200000.00")
    private BigDecimal threshold;

    @TableField("currency")
    @Schema(description = "币种", example = "CNY")
    private String currency;

    @TableField("effective_at")
    private LocalDateTime effectiveAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
