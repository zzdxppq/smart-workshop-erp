package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppMessageApi {
    @GET("app/messages")
    suspend fun listMessages(
        @Query("unreadOnly") unreadOnly: Boolean? = true,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ApiResult<List<AppMessageDto>>

    @POST("app/messages/{id}/read")
    suspend fun markRead(@Path("id") id: Long, @Body body: MarkReadRequest): ApiResult<Unit?>
}

data class MarkReadRequest(val userId: Long)

data class AppMessageDto(
    val id: Long? = null,
    val type: String? = null,
    val title: String? = null,
    val content: String? = null,
    val routeUrl: String? = null,
    val read: Boolean? = null,
)
