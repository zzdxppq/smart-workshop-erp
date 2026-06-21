package com.btsheng.erp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 人事 · 工资条 / 绩效 / 申诉 */
interface HrApi {

    @GET("/hr/payroll/my")
    suspend fun myPayrolls(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 12,
    ): ApiResult<PayrollListDto>

    @GET("/hr/payroll/{id}")
    suspend fun payrollDetail(@Path("id") id: Long): ApiResult<PayrollDto>

    @GET("/hr/performance/my")
    suspend fun myPerformances(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 12,
    ): ApiResult<PerformanceListDto>

    @POST("/hr/performance-appeals")
    suspend fun submitAppeal(@Body body: AppealRequest): ApiResult<AppealDto>

    @GET("/hr/performance-appeals/my")
    suspend fun myAppeals(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ApiResult<AppealListDto>

    @GET("/hr/employees/me")
    suspend fun myEmployee(): ApiResult<EmployeeDto>
}

data class PayrollListDto(val list: List<PayrollDto>? = null)
data class PayrollDto(
    val id: Long? = null,
    val payrollNo: String? = null,
    val periodYear: Int? = null,
    val periodMonth: Int? = null,
    val employeeName: String? = null,
    val baseSalary: Double? = null,
    val piecePay: Double? = null,
    val performanceBonus: Double? = null,
    val overtimePay: Double? = null,
    val deduction: Double? = null,
    val socialInsurance: Double? = null,
    val tax: Double? = null,
    val netSalary: Double? = null,
    val status: String? = null,
)

data class PerformanceListDto(val list: List<PerformanceDto>? = null)
data class PerformanceDto(
    val id: Long? = null,
    val score: Double? = null,
    val grade: String? = null,
    val periodYear: Int? = null,
    val periodMonth: Int? = null,
    val kpiItems: String? = null,
)

data class AppealListDto(val list: List<AppealDto>? = null)
data class AppealDto(
    val id: Long? = null,
    val performanceId: Long? = null,
    val reason: String? = null,
    val status: String? = null,
    val reply: String? = null,
    val periodYear: Int? = null,
    val periodMonth: Int? = null,
)

data class AppealRequest(val performanceId: Long, val reason: String)

data class EmployeeDto(
    val id: Long? = null,
    val employeeNo: String? = null,
    val name: String? = null,
    val department: String? = null,
    val position: String? = null,
)
