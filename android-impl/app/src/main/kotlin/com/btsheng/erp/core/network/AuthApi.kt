package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** 登录 · erp-platform /auth/login */
interface AuthApi {

    @POST("/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResult<LoginResponseData>

    @GET("/auth/me")
    suspend fun me(): ApiResult<UserProfileDto>
}

data class UserProfileDto(
    val id: Long? = null,
    val username: String? = null,
    val realName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val deptId: Long? = null,
    val status: String? = null,
    val roleCodes: List<String>? = null,
)

data class LoginRequest(val username: String, val password: String)

data class LoginResponseData(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val roles: List<String>? = null,
    val user: LoginUser? = null,
)

data class LoginUser(
    val id: Long? = null,
    val username: String? = null,
    val realName: String? = null,
)
