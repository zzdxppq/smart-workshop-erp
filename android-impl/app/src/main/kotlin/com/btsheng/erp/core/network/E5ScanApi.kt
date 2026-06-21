package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** OpenAPI 对齐 · E5 扫码三码 */
interface E5ScanApi {

    @POST("app/workorders/{barcode}/start")
    suspend fun startWorkorder(
        @Path("barcode") barcode: String,
        @Body body: ScanStartBody = ScanStartBody(),
        @Header("X-User-Id") userId: Long = 1L,
    ): ApiResult<Map<String, Any?>>

    @POST("app/workorders/{barcode}/report")
    suspend fun reportWorkorder(
        @Path("barcode") barcode: String,
        @Body body: ScanReportBody,
        @Header("X-User-Id") userId: Long = 1L,
    ): ApiResult<Map<String, Any?>>

    @POST("app/transfer/{barcode}/next")
    suspend fun transferNext(
        @Path("barcode") barcode: String,
        @Body body: ScanTransferBody,
        @Header("X-User-Id") userId: Long = 1L,
    ): ApiResult<Map<String, Any?>>

    @GET("app/production/scan/pending")
    suspend fun listPending(
        @Header("X-User-Id") userId: Long = 1L,
    ): ApiResult<ScanPendingResponse>
}

data class ScanStartBody(
    val stepNo: Int = 1,
    val machineBarcode: String? = null,
)

data class ScanReportBody(
    val qtyDone: Int,
    val qtyOk: Int,
    val qtyScrap: Int,
    val stepNo: Int = 1,
)

data class ScanTransferBody(
    val workorderNo: String,
    val fromStepNo: Int = 1,
    val toStepNo: Int = 2,
)
