package com.btsheng.erp.core.network

import retrofit2.http.GET
import retrofit2.http.Query

/** E1-Workflow · 采购 / 采购主管审批待办 */
interface ApprovalApi {

    @GET("/approvals/pending")
    suspend fun pending(
        @Query("approverUserId") approverUserId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResult<PageResponseDto<ApprovalItemDto>>

    @GET("/approvals/my-pending")
    suspend fun myPending(
        @Query("applicantUserId") applicantUserId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResult<PageResponseDto<ApprovalItemDto>>
}
