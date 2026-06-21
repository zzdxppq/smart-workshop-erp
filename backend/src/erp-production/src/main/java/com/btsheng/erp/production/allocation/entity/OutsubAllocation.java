package com.btsheng.erp.production.allocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "工序分配（outsub_allocation · V1.3.7 生管）")
@TableName("outsub_allocation")
public class OutsubAllocation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("workorder_id")
    private Long workorderId;
    @TableField("process_seq")
    private Integer processSeq;
    @TableField("decision")
    private String decision;
    @TableField("decided_by_user_id")
    private Long decidedByUserId;
    @TableField("decided_at")
    private LocalDateTime decidedAt;
}
