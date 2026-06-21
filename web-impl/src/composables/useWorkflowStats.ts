import { ref } from 'vue'
import { V138WorkflowService } from '@/api/generated/services/V138WorkflowService'
import type { WorkflowEventStats } from '@/api/generated/models/WorkflowEventStats'

/**
 * V1.3.9 Sprint 13 · Story 13.4 · sys_workflow_event 仪表盘 composable
 *
 * 消费 Sprint 10.3 已 ship 的 `GET /workflow/events/stats` 端点
 * 返回 4 图渲染所需的响应结构：totalCount / byEventType / byApproverRole / period
 *
 * 约束说明：
 * - codegen `V138WorkflowService.getWorkflowEventStats(workflowCode, ...)` 第 1 参必填
 * - backend controller `WorkflowEventService.stats()` 对 workflowCode 做非空校验
 *   （不传则 `Result.fail(CODE_PARAM_MISSING, "workflowCode 必填")`）
 * - backend mapper `WHERE workflow_code = #{workflowCode}` 精确匹配 · 不支持多选
 * - 13.4 IMPL：用户在 UI 选择"全部"时, 默认传 PO_APPROVAL 走单端点
 *   （保持与 10.3 端点契约一致 · 13.4 范围内不动 backend 端点契约）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
export function useWorkflowStats() {
  const stats = ref<WorkflowEventStats | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  /**
   * 拉取工作流事件统计
   * @param params.workflowCode 工作流代码（PO_APPROVAL/QUOTE_APPROVAL/OUTSOURCE_APPROVAL）· 留空 = 默认 PO_APPROVAL
   * @param params.approverRole 审批角色（PROCUREMENT_MANAGER/SALES_MANAGER/GM/ADMIN）· 留空 = 全部
   * @param params.startDate 开始日期 YYYY-MM-DD
   * @param params.endDate 结束日期 YYYY-MM-DD
   */
  async function fetchStats(params: {
    workflowCode?: string
    approverRole?: string
    startDate?: string
    endDate?: string
  } = {}) {
    loading.value = true
    error.value = null
    try {
      // backend 10.3 controller 必填 workflowCode · 未选时落 PO_APPROVAL 默认值
      const wfCode = params.workflowCode || 'PO_APPROVAL'
      const response = await V138WorkflowService.getWorkflowEventStats(
        wfCode,
        params.approverRole,
        params.startDate,
        params.endDate,
      )
      // 端点返回 Result<WorkflowEventStats> 结构
      const result = response as unknown as { code: number; data?: WorkflowEventStats; message?: string }
      if (result.code === 0 && result.data) {
        stats.value = result.data
      } else {
        error.value = result.message || '加载审批事件统计失败'
        stats.value = null
      }
    } catch (e: unknown) {
      const err = e as { message?: string; code?: number }
      error.value = err?.message || '网络错误'
      stats.value = null
    } finally {
      loading.value = false
    }
  }

  return { stats, loading, error, fetchStats }
}
