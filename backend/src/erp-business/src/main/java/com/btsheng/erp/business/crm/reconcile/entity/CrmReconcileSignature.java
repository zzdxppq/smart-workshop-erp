package com.btsheng.erp.business.crm.reconcile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "厂商签字扫描件（crm_reconcile_signature · AES-256-GCM 加密）")
@TableName("crm_reconcile_signature")
public class CrmReconcileSignature implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("reconcile_id")            private Long reconcileId;
    @TableField("signer_user_id")          private Long signerUserId;
    @TableField("signer_name")             private String signerName;
    @TableField("signature_image_path")    private String signatureImagePath;
    @TableField("encrypted_data")          private String encryptedData;
    @TableField("iv")                      private String iv;
    @TableField("auth_tag")                private String authTag;
    @TableField("signed_at")               private LocalDateTime signedAt;
}
