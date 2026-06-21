package com.btsheng.erp.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.btsheng.erp.R

/**
 * V1.3.9 Sprint 14 Story 13.6 · 图纸预览 Page Object（android-impl E2E）
 *
 * <p>5 操作：preview / print / download / upload / delete
 * <p>复用 Sprint 8.5 DrawingPreviewFragment 命名规范。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object DrawingPreviewPage {

    fun assertPreviewDisplayed() {
        onView(withId(R.id.previewContainer)).check(matches(isDisplayed()))
    }

    fun tapPreview() {
        onView(withId(R.id.previewButton)).perform(click())
    }

    fun tapPrint() {
        onView(withId(R.id.printButton)).perform(click())
    }

    fun tapDownload() {
        onView(withId(R.id.downloadButton)).perform(click())
    }

    fun tapUpload() {
        onView(withId(R.id.uploadButton)).perform(click())
    }

    fun tapDelete() {
        onView(withId(R.id.deleteButton)).perform(click())
    }

    fun tapDrawingTab() {
        onView(withId(R.id.drawingTab)).perform(click())
    }

    fun assertPrintDialogDisplayed() {
        onView(withId(R.id.printDialog)).check(matches(isDisplayed()))
    }
}
