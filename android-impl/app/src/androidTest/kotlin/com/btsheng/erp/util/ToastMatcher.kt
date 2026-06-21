package com.btsheng.erp.util

import android.view.View
import android.widget.Toast
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * V1.3.9 Sprint 14 Story 13.6 · Toast 匹配器（android-impl E2E）
 *
 * <p>用于验证 DrawPermissionInterceptor 弹出的 Toast 文案。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
class ToastMatcher : TypeSafeMatcher<Root>() {

    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(root: Root): Boolean {
        val type = root.windowLayoutParams?.type ?: return false
        if (type == android.view.WindowManager.LayoutParams.TYPE_TOAST) {
            return true
        }
        // 部分 ROM 上 toast type 为 APPLICATION_OVERLAY 或 PHANTOM
        val windowType = root.windowLayoutParams?.type
        return windowType == android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY ||
                windowType == android.view.WindowManager.LayoutParams.TYPE_PHANTOM
    }

    companion object {
        fun isToast(): Matcher<Root> = ToastMatcher()
    }
}
