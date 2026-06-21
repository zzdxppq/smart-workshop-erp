package com.btsheng.erp.core.network

import retrofit2.http.GET
import retrofit2.http.Path

/** 系统参数 · Story 1.3 / APP 版本检测 */
interface SysParamApi {
    @GET("params/{key}")
    suspend fun getByKey(@Path("key") key: String): ApiResult<SysParamDto>
}

data class SysParamDto(
    val paramKey: String? = null,
    val paramValue: String? = null,
    val paramGroup: String? = null,
    val description: String? = null,
)
