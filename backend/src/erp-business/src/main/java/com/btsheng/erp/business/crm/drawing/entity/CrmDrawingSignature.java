package com.btsheng.erp.business.crm.drawing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.7 · 图纸签字扫描件（AES-256-GCM 加密 · V1.3.6 红线）
 */
@Data
@Schema(description = "图纸签字扫描件（crm_drawing_signature）")
@TableName("crm_drawing_signature")
public class CrmDrawingSignature implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("version") private String version;
    @TableField("signer_user_id") private Long signerUserId;
    @TableField("signature_image_path") private String signatureImagePath;
    @TableField("encrypted_aes_key") private String encryptedAesKey;
    @TableField("iv") private String iv;                            // IV 唯一
            @TableField("signed_at") private LocalDateTime signedAt;
}
