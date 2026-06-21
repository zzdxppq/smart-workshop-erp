package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * V1.3.9 Sprint 12 Story 12.1 · DrawingPermissionDTO
 *
 * <p>permission 端点（GET /drawings/{id}/permission）返回结构
 * <p>任意角色可调（包含 FINANCE 也返 200 + 全 false + scope=NONE）
 * <p>含 5 操作位 + scope + linkedBizIds（按 biz_type 分桶）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Data
@Schema(description = "图纸权限位 DTO")
public class DrawingPermissionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "图纸 ID")
    private Long drawingId;

    @Schema(description = "用户主角色", example = "SALES")
    private String role;

    @Schema(description = "权限 scope",
            example = "ORDER",
            allowableValues = {"ALL", "ORDER", "PO", "INCOMING", "INSPECTION", "WORKORDER_PROCESS", "NONE"})
    private String scope;

    @Schema(description = "5 操作位")
    private PermissionBitsDTO permissions;

    @Schema(description = "关联业务单据 ID 列表（按 bizType 分桶 · SALES/PURCHASER/WAREHOUSE/QC/OPERATOR 有效）")
    private Map<String, List<Long>> linkedBizIds;

    @Schema(description = "权限查询结果缓存过期时间")
    private String expiresAt;

    @Data
    @Schema(description = "5 操作位")
    public static class PermissionBitsDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        @Schema(description = "预览权限")
        private boolean view;
        @Schema(description = "打印权限")
        private boolean print;
        @Schema(description = "下载权限")
        private boolean download;
        @Schema(description = "上传权限")
        private boolean upload;
        @Schema(description = "删除权限")
        private boolean delete;

        public boolean isView() { return view; }
        public void setView(boolean view) { this.view = view; }
        public boolean isPrint() { return print; }
        public void setPrint(boolean print) { this.print = print; }
        public boolean isDownload() { return download; }
        public void setDownload(boolean download) { this.download = download; }
        public boolean isUpload() { return upload; }
        public void setUpload(boolean upload) { this.upload = upload; }
        public boolean isDelete() { return delete; }
        public void setDelete(boolean delete) { this.delete = delete; }
    }
}