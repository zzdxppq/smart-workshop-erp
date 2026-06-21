package com.btsheng.erp.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.btsheng.erp.R

/**
 * V1.3.9 Sprint 14 Story 13.6 · 质检录入 Page Object（android-impl E2E · 13.6 新增）
 *
 * <p>QC 角色专属流程：检验列表 → 检验录入。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object InspectionEntryPage {

    fun assertDisplayed() {
        onView(withId(R.id.inspectionContainer)).check(matches(isDisplayed()))
    }

    fun tapFirstInspection() {
        onView(withId(R.id.inspectionItem0)).perform(click())
    }

    fun fillInspection(passQty: String, failQty: String, remark: String) {
        onView(withId(R.id.passQtyField)).perform(typeText(passQty))
        onView(withId(R.id.failQtyField)).perform(typeText(failQty))
        onView(withId(R.id.remarkField)).perform(typeText(remark))
    }

    fun tapSubmit() {
        onView(withId(R.id.submitInspectionButton)).perform(click())
    }
}
