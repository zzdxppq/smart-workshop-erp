package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal

/** E12-S2 · 仓管扫 WW- 委外到货 */
interface OutsourceReceiveApi {

    @GET("/outsource/{outsourceNo}")
    suspend fun getOutsource(@Path("outsourceNo") outsourceNo: String): ApiResult<OutsourceOrderDto>

    @POST("/outsource/by-no/{outsourceNo}/receive")
    suspend fun receiveByNo(
        @Path("outsourceNo") outsourceNo: String,
        @Body body: OutsourceArriveBody,
        @Header("X-User-Id") userId: Long,
    ): ApiResult<OutsourceOrderDto>
}

data class OutsourceOrderDto(
    val id: Long? = null,
    val outsourceNo: String? = null,
    val workorderNo: String? = null,
    val supplierName: String? = null,
    val processName: String? = null,
    val materialCode: String? = null,
    val qty: Int? = null,
    val status: String? = null,
)

data class OutsourceArriveBody(
    val outsourceNo: String,
    val actualQty: Int,
    val actualWeight: BigDecimal? = null,
    val remark: String? = null,
)
