package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** E7-Quality · APP 品检主力（列表 / 录入 / 提交） */
interface QualityInspectionApi {

    @GET("/quality/inspections")
    suspend fun list(
        @Query("type") type: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("status") status: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResult<InspectionListPage>

    @GET("/quality/inspections/{id}")
    suspend fun detail(@Path("id") id: Long): ApiResult<InspectionDetailDto>

    @POST("/quality/inspections")
    suspend fun create(
        @Body body: InspectionCreateRequestDto,
        @Header("X-User-Id") userId: Long = 1L,
    ): InspectionCreateResponseDto

    @POST("/quality/inspections/{id}/submit")
    suspend fun submit(
        @Path("id") id: Long,
        @Body body: InspectionSubmitRequestDto,
        @Header("X-User-Id") userId: Long = 1L,
    ): ApiResult<Map<String, Any?>>

    @GET("/quality/inspections/{id}/report")
    suspend fun report(@Path("id") id: Long): ApiResult<InspectionDetailDto>

    @GET("/quality/inspections/{id}/concession-approvals")
    suspend fun concessionApprovals(@Path("id") id: Long): ApiResult<List<ConcessionApprovalDto>>

    @POST("/quality/inspections/{id}/approve-concession")
    suspend fun approveConcession(
        @Path("id") id: Long,
        @Body body: ConcessionApproveRequestDto,
        @Header("X-User-Id") userId: Long = 1L,
    ): ApiResult<Map<String, Any?>>
}
