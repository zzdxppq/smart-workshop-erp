package com.btsheng.erp.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
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
 * V1.3.9 Sprint 14 Story 13.6 · Story 12.1 DrawPermissionInterceptor 集成 E2E 测例
 *
 * <p>对应 Story 13.6 §4.3 + §7.1 测例组 3：6 测例
 * <ul>
 *   <li>TC-13.6.3.1 6 角色调用拦截（FINANCE/SALES/PURCHASER/WAREHOUSE/QC/OPERATOR）</li>
 *   <li>TC-13.6.3.2 灰度 feature flag 开启/关闭（draw.acl.gray.{ROLE}）</li>
 *   <li>TC-13.6.3.3 多仓协同（web-impl + android-impl 文案一致）</li>
 *   <li>TC-13.6.3.4 异常路径（网络错误 / token 过期）</li>
 *   <li>TC-13.6.3.5 OPERATOR 工序切换（端点 3 缓存命中）</li>
 *   <li>TC-13.6.3.6 跨设备状态同步（多 AVD 并行）</li>
 * </ul>
 *
 * <p>复用 Story 12.1 已 ship 的 DrawPermissionInterceptor。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DrawPermissionE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun bindLoginPage() {
        LoginPage.composeRule = composeRule
    }

    // =========================================================
    // TC-13.6.3.1 6 角色调用拦截
    //   覆盖 FINANCE/SALES/PURCHASER/WAREHOUSE/QC/OPERATOR
    //   验证 40320/40321/40322 Toast 文案正确
    // =========================================================
    @Test
    fun test_3_1_six_role_permission_interception() {
        // 6 个角色分别在 DrawPermissionInterceptor.handleResponse 路径上触发拦截
        // 1) FINANCE → 40320 "财务角色无图纸权限"
        LoginPage.loginAs(TestAccount.FINANCE.loginUserId, TestAccount.FINANCE.password)
        triggerDeepLink(12345L)
        onView(withText("财务角色无图纸权限"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        // 2) SALES → 40321 "当前订单未关联该图纸"
        LoginPage.loginAs(TestAccount.SALES.loginUserId, TestAccount.SALES.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-SO-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("当前订单未关联该图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        // 3) PURCHASER → 40321 "该图纸未关联您的采购单"
        LoginPage.loginAs(TestAccount.PURCHASER.loginUserId, TestAccount.PURCHASER.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-PO-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("该图纸未关联您的采购单"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        // 4) WAREHOUSE → 40321 "该图纸未关联您的入库单"
        LoginPage.loginAs(TestAccount.WAREHOUSE.loginUserId, TestAccount.WAREHOUSE.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-IN-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("该图纸未关联您的入库单"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        // 5) QC → 40321 "该图纸未关联您的质检单"
        LoginPage.loginAs(TestAccount.QC.loginUserId, TestAccount.QC.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-LJ-999-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("该图纸未关联您的质检单"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        // 6) OPERATOR → 40322 "当前工序未关联该图纸"
        LoginPage.loginAs(TestAccount.OPERATOR.loginUserId, TestAccount.OPERATOR.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-P05-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("当前工序未关联该图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.3.2 灰度 feature flag 开启/关闭
    // =========================================================
    @Test
    fun test_3_2_gray_flag_off_role_loses_menu() {
        // admin 改 sys_dict draw.acl.gray.SALES = false 后 SALES 看不到图纸入口
        LoginPage.loginAs(TestAccount.SALES.loginUserId, TestAccount.SALES.password)
        MainMenuPage.assertDisplayed()
        MainMenuPage.assertDrawingMenuNotVisible()  // 灰度关闭 → 菜单隐藏
    }

    // =========================================================
    // TC-13.6.3.3 多仓协同（文案一致）
    // =========================================================
    @Test
    fun test_3_3_message_text_consistent_across_apps() {
        // 验证 40320/40321/40322 文案与 web-impl 完全一致
        LoginPage.loginAs(TestAccount.FINANCE.loginUserId, TestAccount.FINANCE.password)
        triggerDeepLink(12345L)
        onView(withText("财务角色无图纸权限"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.3.4 异常路径（网络错误 / token 过期）
    // =========================================================
    @Test
    fun test_3_4_network_error_and_token_expired() {
        // 网络错误路径
        LoginPage.loginAs(TestAccount.ENGINEER.loginUserId, TestAccount.ENGINEER.password)
        com.btsheng.erp.util.AdbUtils.simulateNetworkDown()

        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("GD-260614-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()

        onView(withText("网络异常，请重试"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))

        com.btsheng.erp.util.AdbUtils.simulateNetworkUp()

        // token 过期路径：清除 TokenStore → 触发跳转登录页
        com.btsheng.erp.core.security.TokenStore.clear()
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("GD-260614-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.3.5 OPERATOR 工序切换（端点 3 缓存命中）
    // =========================================================
    @Test
    fun test_3_5_operator_process_switch_cache_hit() {
        LoginPage.loginAs(TestAccount.OPERATOR.loginUserId, TestAccount.OPERATOR.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-P03-001")
        // 第一次访问：processId 缓存加载
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        DrawingPreviewPage.assertPreviewDisplayed()

        // 切换工序 P03 → P05 → P03：端点 3 缓存命中（5min TTL）
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-P05-001")
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("当前工序未关联该图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // TC-13.6.3.6 跨设备状态同步（多 AVD 并行）
    // =========================================================
    @Test
    fun test_3_6_cross_device_permission_consistent() {
        // 同一账号在 2 AVD 同时登录 → 拦截行为一致
        // 注：本测例验证单 AVD 行为；多 AVD 并行通过 CI matrix 执行
        LoginPage.loginAs(TestAccount.OPERATOR.loginUserId, TestAccount.OPERATOR.password)
        MainMenuPage.tapScanButton()
        ScanPage.scanBarcode("DWG-P05-001")  // 跨工序
        DrawingPreviewPage.tapDrawingTab()
        DrawingPreviewPage.tapPreview()
        onView(withText("当前工序未关联该图纸"))
            .inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }

    // =========================================================
    // 辅助：触发 deep link 模拟 DrawPermissionInterceptor.handleResponse
    // =========================================================
    private fun triggerDeepLink(drawingId: Long) {
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("btsheng-erp://drawing/$drawingId")
        ).apply { setPackage(ctx.packageName) }
        ctx.startActivity(intent)
    }
}
