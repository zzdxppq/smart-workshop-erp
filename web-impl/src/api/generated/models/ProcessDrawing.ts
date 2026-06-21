/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 工序可访问图纸条目
 */
export type ProcessDrawing = {
    /**
     * 图纸 ID
     */
    drawingId?: number;
    /**
     * 图纸代号
     */
    drawingCode?: string;
    /**
     * 图纸名称
     */
    drawingName?: string;
    /**
     * 缩略图 URL
     */
    thumbnailUrl?: string;
    /**
     * 权限级别（OPERATOR 默认 VIEW）
     */
    permissionLevel?: 'VIEW' | 'PRINT' | 'EDIT';
};

