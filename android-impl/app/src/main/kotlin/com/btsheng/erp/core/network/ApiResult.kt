package com.btsheng.erp.core.network

data class ApiResult<T>(
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null,
) {
    val ok: Boolean get() = code == 0 || code == 200
}
