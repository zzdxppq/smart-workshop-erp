/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 可访问图纸条目
 */
export type AccessibleDrawing = {
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
     * 版本号
     */
    version?: string;
    /**
     * 缩略图 URL
     */
    thumbnailUrl?: string;
    /**
     * 权限级别（5 操作矩阵裁剪）
     */
    permissionLevel?: 'VIEW' | 'PRINT' | 'EDIT';
};

