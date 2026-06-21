/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 5 操作位（7 角色 × 5 操作矩阵）
 */
export type DrawingPermissionBits = {
    /**
     * 预览权限
     */
    view?: boolean;
    /**
     * 打印权限
     */
    print?: boolean;
    /**
     * 下载原文件权限（仅 ENGINEER）
     */
    download?: boolean;
    /**
     * 上传权限（仅 ENGINEER）
     */
    upload?: boolean;
    /**
     * 删除权限（仅 ENGINEER）
     */
    delete?: boolean;
};

