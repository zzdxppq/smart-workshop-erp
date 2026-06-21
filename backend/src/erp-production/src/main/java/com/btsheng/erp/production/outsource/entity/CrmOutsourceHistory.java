package com.btsheng.erp.production.outsource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "委外历史（crm_outsource_history）")
@TableName("crm_outsource_history")
public class CrmOutsourceHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("outsource_no")       private String outsourceNo;
    @TableField("operation")          private String operation;
    @TableField("operator_user_id")   private Long operatorUserId;
    @TableField("operated_at")        private LocalDateTime operatedAt;
    @TableField("from_status")        private String fromStatus;
    @TableField("to_status")          private String toStatus;
    @TableField("note")               private String note;
}
