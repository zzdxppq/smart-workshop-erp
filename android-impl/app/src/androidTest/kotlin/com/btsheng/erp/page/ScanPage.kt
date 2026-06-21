package com.btsheng.erp.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.btsheng.erp.R
import com.btsheng.erp.util.AdbUtils

/**
 * V1.3.9 Sprint 14 Story 13.6 · 扫码 Page Object（android-impl E2E）
 *
 * <p>5 类码路由：GD- 工单 / LZ- 流转 / SB- 设备 / WL- 物料 / WW- 委外
 * <p>复用 Sprint 8.5 命名规范，通过 AdbUtils 模拟 ZXing 扫码输入。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object ScanPage {

    fun assertDisplayed() {
        onView(withId(R.id.scanContainer)).check(matches(isDisplayed()))
    }

    fun scanBarcode(barcode: String) {
        AdbUtils.injectScanResult(barcode)
        onView(withId(R.id.confirmScanButton)).perform(click())
    }

    fun scanManually(barcode: String) {
        onView(withId(R.id.manualInput)).perform(typeText(barcode))
        onView(withId(R.id.confirmScanButton)).perform(click())
    }

    fun assertRouteTitle(title: String) {
        onView(withText(title)).check(matches(isDisplayed()))
    }

    fun cancel() {
        onView(withId(R.id.cancelScanButton)).perform(click())
    }
}
