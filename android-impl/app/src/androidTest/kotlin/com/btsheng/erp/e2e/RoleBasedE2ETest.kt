package com.btsheng.erp.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.btsheng.erp.R
import com.btsheng.erp.feature.MainActivity
import com.btsheng.erp.page.DrawingPreviewPage
import com.btsheng.erp.page.LoginPage
import com.btsheng.erp.page.MainMenuPage
import com.btsheng.erp.page.ScanPage
import com.btsheng.erp.util.TestAccount
import com.btsheng.erp.util.ToastMatcher
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * V1.3.9 Sprint 14 Story 13.6 · 7 角色 connectedAndroidTest E2E 测例（android-impl）
 *
 * <p>对应 Story 13.6 §4.1 + §7.1 测试设计：16 测例（7 业务角色 × 2 + FINANCE 验证 2）
 * <ul>
 *   <li>TC-13.6.1.1 ~ 1.14：7 业务角色 × 2（1 流程 + 1 拦截）</li>
 *   <li>TC-13.6.1.15 ~ 1.16：FINANCE 验证（菜单不可见 + deep link 拦截）</li>
 * </ul>
 *
 * <p>测试账号矩阵见 {@link TestAccount}（8 账号：7 业务角色 + FINANCE）。
 * <p>Page Object 复用 Sprint 8.5 + 10.2 命名规范。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RoleBasedE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun bindLoginPage() {
        LoginPage.composeRule = composeRule
    }

    // =========================================================
    // TC-13.6.1.1 ENGINEER 完整流程（5 全成功）
    // =========================================================
    @Test
    fun test_1_1_engineer_full_access() {
        LoginPage.loginAs(TestAccount.ENGINEER.loginUserId, TestAccount.ENGINEER.password)
        MainMenuPage.assertDisplayed()

        // 扫码 → 工单 GD-260614-001
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("GD-260614-001")

        // 图纸 Tab → 5 全成功
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()

        DrawingPreviewPage.tapPrint()
        DrawingPreviewPage.assertPrintDialogDisplayed()

        DrawingPreviewPage.tapDownload()
        onView(withText("下载成功")).inRoot(ToastMatcher()).check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.2 ENGINEER 拦截验证（admin 拒绝 40301）
    // =========================================================
    @Test
    fun test_1_2_engineer_delete_admin_drawing_denied() {
        LoginPage.loginAs(TestAccount.ENGINEER.loginUserId, TestAccount.ENGINEER.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-ADMIN-001")  // admin 上传的图纸
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapDelete()

        onView(withText("权限不足：仅管理员可删除他人图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.3 PROD_PLANNER 工单流程（2 全成功）
    // =========================================================
    @Test
    fun test_1_3_prod_planner_workorder_flow() {
        LoginPage.loginAs(TestAccount.PROD_PLANNER.loginUserId, TestAccount.PROD_PLANNER.password)
        MainMenuPage.assertDisplayed()

        // 工单列表 → 工序排产 → 看板 → 图纸预览/打印
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("GD-260614-002")  // 工单
        ScanPage.assertRouteTitle("工序排产")

        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()
        DrawingPreviewPage.tapPrint()
        DrawingPreviewPage.assertPrintDialogDisplayed()
    }

    // =========================================================
    // TC-13.6.1.4 PROD_PLANNER 下载拦截（403 DRAW_DOWNLOAD_DENIED）
    // =========================================================
    @Test
    fun test_1_4_prod_planner_download_denied() {
        LoginPage.loginAs(TestAccount.PROD_PLANNER.loginUserId, TestAccount.PROD_PLANNER.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("GD-260614-002")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapDownload()

        onView(withText("生产计划员无下载权限"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.5 SALES 关联订单（200）
    // =========================================================
    @Test
    fun test_1_5_sales_related_order_ok() {
        LoginPage.loginAs(TestAccount.SALES.loginUserId, TestAccount.SALES.password)
        MainMenuPage.assertDisplayed()

        // SALES order=100 → 关联图纸
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-SO-100-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()
    }

    // =========================================================
    // TC-13.6.1.6 SALES 不关联订单（40321 DRAWING_FORBIDDEN_ORDER）
    // =========================================================
    @Test
    fun test_1_6_sales_unrelated_order_denied() {
        LoginPage.loginAs(TestAccount.SALES.loginUserId, TestAccount.SALES.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-SO-200-001")  // SALES 订单 100 → 扫码 200 关联图纸
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()

        onView(withText("当前订单未关联该图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.7 PURCHASER 关联 PO（200）
    // =========================================================
    @Test
    fun test_1_7_purchaser_related_po_ok() {
        LoginPage.loginAs(TestAccount.PURCHASER.loginUserId, TestAccount.PURCHASER.password)
        MainMenuPage.assertDisplayed()
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-PO-100-001")  // PURCHASER PO 100 关联
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()
    }

    // =========================================================
    // TC-13.6.1.8 PURCHASER 不关联 PO（40321）
    // =========================================================
    @Test
    fun test_1_8_purchaser_unrelated_po_denied() {
        LoginPage.loginAs(TestAccount.PURCHASER.loginUserId, TestAccount.PURCHASER.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-PO-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()

        onView(withText("该图纸未关联您的采购单"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.9 WAREHOUSE 关联入库单（200）
    // =========================================================
    @Test
    fun test_1_9_warehouse_related_incoming_ok() {
        LoginPage.loginAs(TestAccount.WAREHOUSE.loginUserId, TestAccount.WAREHOUSE.password)
        MainMenuPage.assertDisplayed()
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-IN-100-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()
    }

    // =========================================================
    // TC-13.6.1.10 WAREHOUSE 不关联入库单（40321）
    // =========================================================
    @Test
    fun test_1_10_warehouse_unrelated_incoming_denied() {
        LoginPage.loginAs(TestAccount.WAREHOUSE.loginUserId, TestAccount.WAREHOUSE.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-IN-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()

        onView(withText("该图纸未关联您的入库单"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.11 QC 关联质检单（200）
    // =========================================================
    @Test
    fun test_1_11_qc_related_inspection_ok() {
        LoginPage.loginAs(TestAccount.QC.loginUserId, TestAccount.QC.password)
        MainMenuPage.assertDisplayed()
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-LJ-100-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()
    }

    // =========================================================
    // TC-13.6.1.12 QC 不关联质检单（40321）
    // =========================================================
    @Test
    fun test_1_12_qc_unrelated_inspection_denied() {
        LoginPage.loginAs(TestAccount.QC.loginUserId, TestAccount.QC.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-LJ-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()

        onView(withText("该图纸未关联您的质检单"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.13 OPERATOR 工序关联（200）· process=P03
    // =========================================================
    @Test
    fun test_1_13_operator_current_process_ok() {
        LoginPage.loginAs(TestAccount.OPERATOR.loginUserId, TestAccount.OPERATOR.password)
        MainMenuPage.assertDisplayed()
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-P03-001")  // 当前工序 P03 关联图纸
        ScanPage.assertRouteTitle("工序 P03")

        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()
    }

    // =========================================================
    // TC-13.6.1.14 OPERATOR 工序不关联（40322 DRAWING_FORBIDDEN_PROCESS）
    // =========================================================
    @Test
    fun test_1_14_operator_other_process_denied() {
        LoginPage.loginAs(TestAccount.OPERATOR.loginUserId, TestAccount.OPERATOR.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-P05-001")  // 当前工序 P03 → 访问 P05 关联图纸
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()

        onView(withText("当前工序未关联该图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.1.15 FINANCE 菜单不可见（0 权限）
    // =========================================================
    @Test
    fun test_1_15_finance_drawing_menu_hidden() {
        LoginPage.loginAs(TestAccount.FINANCE.loginUserId, TestAccount.FINANCE.password)
        MainMenuPage.assertDisplayed()
        MainMenuPage.assertDrawingMenuNotVisible()  // 图纸菜单完全隐藏
    }

    // =========================================================
    // TC-13.6.1.16 FINANCE deep link 拦截（40320 DRAWING_FORBIDDEN_ROLE）
    // =========================================================
    @Test
    fun test_1_16_finance_deep_link_denied() {
        LoginPage.loginAs(TestAccount.FINANCE.loginUserId, TestAccount.FINANCE.password)
        MainMenuPage.assertDisplayed()

        // 手动 deep link 触发 DrawPermissionInterceptor
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("btsheng-erp://drawing/12345")
        ).apply { setPackage(ctx.packageName) }
        ctx.startActivity(intent)

        onView(withText("财务角色无图纸权限"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // 业务链路辅助步骤（合并到主测例步骤内 · 不单独计测例）
    // 角色 7 测例角色测例已覆盖主链路：
    //   ENGINEER    → 登录 → 扫码 → 预览/打印/下载
    //   PROD_PLANNER → 登录 → 工单 → 预览/打印
    //   SALES        → 登录 → 扫码 → 关联图纸预览
    //   PURCHASER   → 登录 → 扫码 → 关联图纸预览
    //   WAREHOUSE   → 登录 → 扫码 → 关联图纸预览
    //   QC          → 登录 → 扫码 → 关联图纸预览
    //   OPERATOR    → 登录 → 扫码 → 工序图纸预览
    // =========================================================
}
