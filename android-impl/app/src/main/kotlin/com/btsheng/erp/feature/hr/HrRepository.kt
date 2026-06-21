package com.btsheng.erp.feature.hr

import com.btsheng.erp.core.network.AppealRequest
import com.btsheng.erp.core.network.HrApi
import com.btsheng.erp.core.network.PayrollDto
import com.btsheng.erp.core.network.PerformanceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrRepository @Inject constructor(private val hrApi: HrApi) {

    suspend fun loadPayrolls(year: Int? = null, month: Int? = null): List<PayrollDto> {
        val r = hrApi.myPayrolls(year = year, month = month)
        if (!r.ok) throw IllegalStateException(mapHrError(r.message))
        return r.data?.list.orEmpty()
    }

    suspend fun loadPerformances(year: Int? = null, month: Int? = null): List<PerformanceDto> {
        val r = hrApi.myPerformances(year = year, month = month)
        if (!r.ok) throw IllegalStateException(mapHrError(r.message))
        return r.data?.list.orEmpty()
    }

    private fun mapHrError(raw: String?): String = when {
        raw?.contains("EMPLOYEE_NOT_FOUND", ignoreCase = true) == true ->
            "未找到员工档案，请联系人事在系统中绑定您的登录账号"
        else -> raw ?: "加载失败"
    }

    suspend fun submitAppeal(performanceId: Long, reason: String) {
        val r = hrApi.submitAppeal(AppealRequest(performanceId, reason))
        if (!r.ok) throw IllegalStateException(r.message ?: "申诉提交失败")
    }

    fun payslipPdfUrl(payrollId: Long, baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/hr/payroll/$payrollId/slip.pdf"
}
