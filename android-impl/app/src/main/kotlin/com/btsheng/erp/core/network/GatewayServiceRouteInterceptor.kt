package com.btsheng.erp.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** OkHttp 拦截器 · 自动补 /erp-{service} 前缀 */
class GatewayServiceRouteInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        val newPath = GatewayServiceRoute.resolveGatewayPath(path)
        if (newPath == path) return chain.proceed(req)
        val newUrl = req.url.newBuilder().encodedPath(newPath).build()
        return chain.proceed(req.newBuilder().url(newUrl).build())
    }
}
