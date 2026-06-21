package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** E4-S2 · APP 扫码出入库（Story 1.12） */
interface WarehouseScanApi {

    @POST("/app/scan/inbound")
    suspend fun scanInbound(
        @Body body: ScanInboundBody,
        @Header("X-User-Id") userId: Long,
    ): ApiResult<ScanOpResponse>

    @POST("/app/scan/outbound")
    suspend fun scanOutbound(
        @Body body: ScanOutboundBody,
        @Header("X-User-Id") userId: Long,
    ): ApiResult<ScanOpResponse>
}

data class ScanInboundBody(
    val barcodeNo: String,
    val locationCode: String,
    val qty: Int,
    val batchNo: String? = null,
    val clientId: String? = null,
    val clientScannedAt: Long? = null,
)

data class ScanOutboundBody(
    val barcodeNo: String,
    val workorderNo: String,
    val qty: Int,
    val locationCode: String? = null,
    val clientId: String? = null,
)

data class ScanOpResponse(
    val scanNo: String? = null,
    val scanType: String? = null,
    val barcodeNo: String? = null,
    val materialCode: String? = null,
    val locationCode: String? = null,
    val qty: Int? = null,
    val syncStatus: String? = null,
)
