package com.btsheng.erp.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.btsheng.erp.R

/**
 * V1.3.9 Sprint 14 Story 13.6 · 询价/采购 Page Object（android-impl E2E · 13.6 新增）
 *
 * <p>PURCHASER 角色专属流程：询价列表 → 中标 → 采购单。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object InquiryListPage {

    fun assertDisplayed() {
        onView(withId(R.id.inquiryListContainer)).check(matches(isDisplayed()))
    }

    fun tapFirstInquiry() {
        onView(withId(R.id.inquiryItem0)).perform(click())
    }

    fun tapBid() {
        onView(withId(R.id.bidButton)).perform(click())
    }

    fun tapGeneratePO() {
        onView(withId(R.id.generatePoButton)).perform(click())
    }
}
