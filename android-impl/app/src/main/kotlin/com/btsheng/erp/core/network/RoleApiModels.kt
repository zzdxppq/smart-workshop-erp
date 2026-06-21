package com.btsheng.erp.core.network

/** E5 · 扫码待办（AC-5.2.4） */
data class ScanPendingResponse(
    val pendingStart: List<ScanPendingItem>? = null,
    val pendingReport: List<ScanPendingItem>? = null,
    val pendingStation: List<ScanPendingItem>? = null,
)

data class ScanPendingItem(
    val workorderNo: String? = null,
    val productName: String? = null,
    val stepNo: Int? = null,
    val stepName: String? = null,
    val equipmentType: String? = null,
    val status: String? = null,
    val scheduledStart: String? = null,
    val priority: String? = null,
)

/** E11 · 生产看板 */
data class ProductionDashboardRow(
    val id: Long? = null,
    val workorderNo: String? = null,
    val productName: String? = null,
    val workorderStatus: String? = null,
    val qtyPlanned: Int? = null,
    val qtyCompleted: Int? = null,
    val progress: Double? = null,
    val alertType: String? = null,
    val alertMessage: String? = null,
)

/** E7 · 品质检验 */
data class InspectionListPage(
    val items: List<InspectionListItem>? = null,
    val records: List<InspectionListItem>? = null,
    val total: Int? = null,
)

data class InspectionListItem(
    val id: Long? = null,
    val inspectionNo: String? = null,
    val type: String? = null,
    val materialCode: String? = null,
    val workOrderNo: String? = null,
    val processName: String? = null,
    val qty: Int? = null,
    val result: String? = null,
)

data class InspectionDetailDto(
    val id: Long? = null,
    val inspectionNo: String? = null,
    val type: String? = null,
    val materialCode: String? = null,
    val materialName: String? = null,
    val workOrderNo: String? = null,
    val processName: String? = null,
    val qty: Int? = null,
    val passQty: Int? = null,
    val failQty: Int? = null,
    val result: String? = null,
    val remark: String? = null,
    val items: List<InspectionDetailItemDto>? = null,
)

data class InspectionDetailItemDto(
    val id: Long? = null,
    val itemName: String? = null,
    val standard: String? = null,
    val actual: String? = null,
    val result: String? = null,
    val severity: String? = null,
    val defectDesc: String? = null,
)

data class InspectionCreateRequestDto(
    val materialCode: String,
    val inspectionType: String,
    val workOrderNo: String? = null,
    val processName: String? = null,
    val remark: String? = null,
    val inspectItems: List<InspectionCreateItemDto>,
)

data class InspectionCreateItemDto(
    val itemName: String,
    val standard: String? = null,
    val measuredValue: String? = null,
    val result: String? = null,
)

data class InspectionCreateResponseDto(
    val inspectionId: Long? = null,
    val inspectionNo: String? = null,
    val status: String? = null,
)

data class InspectionSubmitRequestDto(
    val conclusion: String,
    val items: List<InspectionSubmitItemDto>,
    val remark: String? = null,
    val rejectReason: String? = null,
    val overallResult: String? = null,
    val disposition: String? = null,
    val defectQty: Int? = null,
    val conditionalReason: String? = null,
)

data class ConcessionApprovalDto(
    val id: Long? = null,
    val approverRole: String? = null,
    val approverRoleLabel: String? = null,
    val approvalStatus: String? = null,
    val comment: String? = null,
)

data class ConcessionApproveRequestDto(
    val approverRole: String,
    val action: String,
    val comment: String? = null,
)

data class InspectionSubmitItemDto(
    val id: Long? = null,
    val itemName: String? = null,
    val measuredValue: String? = null,
    val result: String,
    val severity: String? = null,
    val defectDesc: String? = null,
)

/** E1 · 审批 */
data class PageResponseDto<T>(
    val records: List<T>? = null,
    val total: Long? = null,
    val pageNum: Long? = null,
    val pageSize: Long? = null,
)

data class ApprovalItemDto(
    val id: Long? = null,
    val bizType: String? = null,
    val bizId: String? = null,
    val workflowCode: String? = null,
    val status: String? = null,
    val comment: String? = null,
    val reason: String? = null,
    val isOverdue: Boolean? = null,
    val createdAt: String? = null,
    val timeoutAt: String? = null,
)
