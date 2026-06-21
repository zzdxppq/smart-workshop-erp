package com.btsheng.erp.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.btsheng.erp.R

/**
 * V1.3.9 Sprint 14 Story 13.6 · 报价新建 Page Object（android-impl E2E · 13.6 新增）
 *
 * <p>SALES 角色专属流程：报价新建 → 提交审批 → 报价转订单。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object QuoteCreatePage {

    fun assertDisplayed() {
        onView(withId(R.id.quoteCreateContainer)).check(matches(isDisplayed()))
    }

    fun fillQuote(customer: String, product: String, quantity: String, unitPrice: String) {
        onView(withId(R.id.customerField)).perform(typeText(customer))
        onView(withId(R.id.productField)).perform(typeText(product))
        onView(withId(R.id.quantityField)).perform(typeText(quantity))
        onView(withId(R.id.unitPriceField)).perform(typeText(unitPrice))
    }

    fun tapSave() {
        onView(withId(R.id.saveQuoteButton)).perform(click())
    }

    fun tapSubmit() {
        onView(withId(R.id.submitQuoteButton)).perform(click())
    }

    fun tapConvertToOrder() {
        onView(withId(R.id.convertToOrderButton)).perform(click())
    }
}
