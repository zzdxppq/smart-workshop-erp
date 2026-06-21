package com.btsheng.erp.core.network

import com.btsheng.erp.core.security.SessionStore
import com.btsheng.erp.core.security.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/** 自动附加 JWT 与 X-User-Id（与 Web axios 拦截器对齐） */
class AuthHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val builder = req.newBuilder()
        TokenStore.load()?.accessToken?.takeIf { it.isNotBlank() }?.let {
            builder.header("Authorization", "Bearer $it")
        }
        SessionStore.session?.userId?.let {
            builder.header("X-User-Id", it.toString())
        }
        return chain.proceed(builder.build())
    }
}
