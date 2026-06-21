package com.btsheng.erp.feature.v138

import com.btsheng.erp.core.network.ApiResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.time.LocalDateTime

/**
 * V1.3.8 Sprint 7 · android-impl 后端 API 调用接口
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
interface ApiClient {

    // 3.1 分批到货
    @POST("/incoming/batch-create")
    suspend fun batchCreate(
        @Body req: BatchCreateRequest
    ): ApiResult<BatchCreateResponse>

    @GET("/incoming/po-status/{poId}")
    suspend fun getPoStatus(@retrofit2.http.Path("poId") poId: Long): ApiResult<PoStatusResponse>

    // 3.2 物料码
    @POST("/material-barcode/generate")
    suspend fun generateMaterialBarcode(@Body req: MaterialBarcodeRequest): MaterialBarcodeResponse

    @GET("/material-barcode/parse")
    suspend fun parseMaterialBarcode(@Query("barcode") barcode: String): ApiResult<MaterialBarcodeParseResponse>

    // 4.1 无订单采购
    @POST("/purchase/no-order")
    suspend fun createNoOrderPurchase(@Body req: NoOrderPurchaseRequest): NoOrderPurchaseResponse

    @GET("/purchase/reasons")
    suspend fun getPurchaseReasons(): List<PurchaseReason>

    // 4.2 审批路由
    @POST("/approval/route-preview")
    suspend fun previewApprovalRoute(@Body req: ApprovalRouteRequest): ApprovalRouteResponse

    @GET("/roles/procurement-manager-perms")
    suspend fun getProcurementManagerPerms(): List<String>

    // 4.3 总经理报表
    @GET("/reports/gm-summary")
    suspend fun getGmSummary(@Query("period") period: String): GmSummaryResponse

    // V1.3.9 Sprint 12 Story 12.3 · 标签模板
    @GET("/label-templates")
    suspend fun listLabelTemplates(
        @Query("type") type: String? = null,
        @Query("tenantId") tenantId: Long = 1L
    ): LabelTemplateListResponse

    @POST("/label-templates/preview")
    suspend fun previewLabel(
        @retrofit2.http.Body req: LabelPreviewRequest,
        @Query("tenantId") tenantId: Long = 1L
    ): LabelPreviewResponseData

    // V1.3.9 Sprint 12 Story 12.4 · 双模式打印（android-impl 仅支持 PDF_BROWSER）
    @POST("/print/labels/pdf-a4")
    suspend fun printPdfA4(
        @retrofit2.http.Body req: PrintPdfA4Request,
        @retrofit2.http.Header("X-User-Id") userId: Long = 1L,
        @retrofit2.http.Header("X-User-Name") userName: String = "操作员",
        @Query("tenantId") tenantId: Long = 1L
    ): PrintPdfA4Response

    @GET("/print/logs")
    suspend fun listPrintLogs(
        @Query("codeType") codeType: String? = null,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("tenantId") tenantId: Long = 1L
    ): PrintLogListResponse

    // ===== V1.3.9 Sprint 12 Story 12.1 图纸权限矩阵 =====

    /**
     * 查询当前用户对图纸的权限位（任意角色可调）
     * @param drawingId 图纸 ID
     * @param bearerToken JWT bearer token
     */
    @GET("/drawings/{id}/permission")
    suspend fun getDrawingPermission(
        @retrofit2.http.Path("id") drawingId: Long,
        @retrofit2.http.Header("Authorization") bearerToken: String
    ): DrawingPermissionApiResponse

    /**
     * 查询图纸 PDF 流（鉴权由 backend @drawingAuthz.canView 强制）
     * @param drawingId 图纸 ID
     * @param bearerToken JWT bearer token
     * @param resolution LOW / MEDIUM / HIGH
     */
    @GET("/drawings/{id}/preview")
    suspend fun previewDrawing(
        @retrofit2.http.Path("id") drawingId: Long,
        @retrofit2.http.Header("Authorization") bearerToken: String,
        @retrofit2.http.Query("resolution") resolution: String = "MEDIUM"
    ): retrofit2.Response<okhttp3.ResponseBody>

    /**
     * 列出 sys_dict 条目（用于 APP 端 feature flag 拉取 · 灰度 4 阶段）
     */
    @GET("/dicts")
    suspend fun listDrawingFeatureFlags(
        @Query("type") type: String = "DRAWING_ACL_FEATURE_FLAG",
        @retrofit2.http.Header("Authorization") bearerToken: String
    ): DictListApiResponse

    /**
     * OPERATOR 当前工序查询（5min Redis 缓存由 backend 提供）
     * @param bearerToken JWT bearer token
     */
    @GET("/workorders/current-process")
    suspend fun getCurrentProcess(
        @retrofit2.http.Header("Authorization") bearerToken: String
    ): CurrentProcessApiResponse

    /**
     * 工单工序树（E5-S6 · 替代本地 mock 工序列表）
     */
    @GET("/workorders/{workorderId}/processes")
    suspend fun listWorkorderProcesses(
        @retrofit2.http.Path("workorderId") workorderId: Long,
        @retrofit2.http.Header("Authorization") bearerToken: String
    ): WorkorderProcessListApiResponse
}

// ===== V1.3.9 Sprint 12 Story 12.4 DTO =====

data class PrintPdfA4Request(
    val items: List<PrintPdfA4Item>,
    val remark: String? = null
)

data class PrintPdfA4Item(
    val templateCode: String,
    val qrContent: String,
    val lines: List<String> = emptyList(),
    val colorBarHex: String? = null
)

data class PrintPdfA4Response(
    val printLogId: Long,
    val logNo: String,
    val pdfBase64: String,
    val bytes: Int,
    val contentType: String,
    val filename: String
)

data class PrintLogListResponse(
    val records: List<PrintLogEntry>,
    val total: Int
)

data class PrintLogEntry(
    val id: Long,
    val logNo: String,
    val codeType: String,
    val codeValue: String,
    val printMode: String,
    val status: String,
    val printedAt: String,
    val referenceLogId: Long? = null
)

// ===== DTO =====

data class BatchCreateRequest(
    val poId: Long,
    val arrivedAt: LocalDateTime,
    val items: List<BatchItem>
)

data class BatchItem(
    val materialId: Long,
    val quantity: Int,
    val poItemId: Long? = null
)

data class BatchCreateResponse(
    val batches: List<BatchInfo>? = null,
    val poStatusAfter: String? = null,
    val qualityOrders: List<String>? = null,
)

data class BatchInfo(
    val batchNo: String,
    val materialId: Long,
    val quantity: Int
)

data class PoStatusResponse(
    val poId: Long? = null,
    val poStatus: String? = null,
    val items: List<PoStatusItem>? = null,
)

data class PoStatusItem(
    val materialId: Long,
    val ordered: Int,
    val arrived: Int,
    val batchCount: Int,
    val qualityStatus: String
)

data class MaterialBarcodeRequest(
    val materialId: Long,
    val batchId: Long,
    val materialNo: String
)

data class MaterialBarcodeResponse(
    val barcodeNo: String,
    val isNew: Boolean,
    val oldBarcode: String
)

data class MaterialBarcodeParseResponse(
    val materialId: Long,
    val materialNo: String,
    val batchId: Long,
    val batchNo: String,
    val arrivedAt: LocalDateTime,
    val qualityStatus: String
)

data class NoOrderPurchaseRequest(
    val purchaseReason: String,
    val items: List<NoOrderPurchaseItem>,
    val supplierId: Long,
    val remark: String?
)

data class NoOrderPurchaseItem(
    val materialId: Long,
    val quantity: Int,
    val estimatedPrice: Double
)

data class NoOrderPurchaseResponse(
    val poId: Long,
    val poNo: String,
    val sourceType: String,
    val purchaseReason: String,
    val approvalRoute: String,
    val estimatedTotal: Double
)

data class PurchaseReason(
    val code: String,
    val name: String,
    val color: String
)

data class ApprovalRouteRequest(
    val amount: Double,
    val category: String?,
    val supplierStatus: String?,
    val urgency: String?
)

data class ApprovalRouteResponse(
    val route: List<String>,
    val matchedThresholds: List<String>,
    val estimatedSigners: Int,
    val compatibleLegacyRoute: List<String>?
)

data class GmSummaryResponse(
    val period: String,
    val noOrderPoCount: Int,
    val noOrderPoAmount: Double,
    val urgentReplenishCount: Int,
    val amountThresholdPassedRate: Double,
    val procurementManagerWorkload: Int,
    val outsourceCostRatio: Double
)

// ===== V1.3.9 Sprint 12 Story 12.3 标签模板 DTO =====

data class LabelTemplateListResponse(
    val templates: List<LabelTemplateDto>,
    val companyName: String?
)

data class LabelTemplateDto(
    val type: String,
    val name: String,
    val prefix: String,
    val colorStrip: String,
    val reuseFrom: String?,
    val layout: Any? = null,
    val dpi: Int = 300,
    val enabled: Boolean = true,
    val qrExample: String? = null
)

data class LabelPreviewRequest(
    val type: String,
    val data: LabelPreviewData,
    val format: String = "PNG"
)

data class LabelPreviewData(
    val qrContent: String,
    val lines: List<String> = emptyList(),
    val factoryName: String? = null
)

data class LabelPreviewResponseData(
    val type: String,
    val format: String,
    val base64: String,
    val contentType: String,
    val sizeBytes: Int,
    val renderedAt: String
)

// ===== V1.3.9 Sprint 12 Story 12.1 图纸权限 DTO =====

data class DrawingPermissionApiResponse(
    val code: Int,
    val message: String? = null,
    val data: DrawingPermissionData? = null
)

data class DrawingPermissionData(
    val drawingId: Long,
    val role: String? = null,
    val scope: String? = null,
    val permissions: DrawingPermissionBitsData? = null,
    val linkedBizIds: Map<String, List<Long>>? = null,
    val expiresAt: String? = null
)

data class DrawingPermissionBitsData(
    val view: Boolean = false,
    val print: Boolean = false,
    val download: Boolean = false,
    val upload: Boolean = false,
    val delete: Boolean = false
)

// ===== V1.3.9 Sprint 12 Story 12.1 OPERATOR 灰度 DTO =====

/**
 * sys_dict 列表响应（platform /dicts）
 */
data class DictListApiResponse(
    val code: Int,
    val message: String? = null,
    val data: List<DictEntry>? = null
)

data class DictEntry(
    val dictType: String? = null,
    val dictCode: String? = null,
    val dictLabel: String? = null,
    val sort: Int? = null,
    val status: String? = null
)

/**
 * OPERATOR 当前工序响应（5min Redis 缓存由 backend 提供）
 */
data class CurrentProcessApiResponse(
    val code: Int,
    val message: String? = null,
    val data: CurrentProcessData? = null
)

data class CurrentProcessData(
    val userId: Long? = null,
    val workorderId: Long? = null,
    val drawingId: Long? = null,
    val processId: Long? = null,
    val processNo: String? = null,
    val processName: String? = null,
    val processSeq: Int? = null,
    val workorderNo: String? = null,
    val cached: Boolean = false,
    val cachedAt: String? = null
)

data class WorkorderProcessListApiResponse(
    val code: Int,
    val message: String? = null,
    val data: List<WorkorderProcessItem>? = null
)

data class WorkorderProcessItem(
    val id: Long? = null,
    val workorderId: Long? = null,
    val workorderNo: String? = null,
    val processSeq: Int? = null,
    val processCode: String? = null,
    val processName: String? = null,
    val status: String? = null,
    val isOutsource: Int? = null
)