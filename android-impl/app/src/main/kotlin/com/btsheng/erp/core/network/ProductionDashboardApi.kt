package com.btsheng.erp.core.network

import retrofit2.http.GET
import retrofit2.http.Query

/** E11-Dashboard-Production · 生管工单预警 / 进度 */
interface ProductionDashboardApi {

    @GET("/dashboard/production/alerts")
    suspend fun alerts(@Query("limit") limit: Int = 20): ApiResult<List<ProductionDashboardRow>>

    @GET("/dashboard/production/workorders")
    suspend fun workorders(@Query("limit") limit: Int = 50): ApiResult<List<ProductionDashboardRow>>
}
