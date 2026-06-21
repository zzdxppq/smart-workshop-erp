package com.btsheng.erp.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.btsheng.erp.R

/**
 * V1.3.9 Sprint 14 Story 13.6 · 主菜单 Page Object（android-impl E2E）
 *
 * <p>复用 Sprint 8.5 + Sprint 10.2 命名规范。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object MainMenuPage {

    fun assertDisplayed() {
        onView(withId(R.id.mainMenu)).check(matches(isDisplayed()))
    }

    fun tapScanButton() {
        onView(withId(R.id.scanButton)).perform(click())
    }

    fun tapDrawingMenu() {
        onView(withId(R.id.drawingMenu)).perform(click())
    }

    fun assertDrawingMenuVisible() {
        onView(withId(R.id.drawingMenu)).check(matches(isDisplayed()))
    }

    fun assertDrawingMenuNotVisible() {
        onView(withId(R.id.drawingMenu)).check(doesNotExist())
    }

    fun tapMenuItem(label: String) {
        onView(withText(label)).perform(click())
    }

    fun tapLogout() {
        onView(withId(R.id.logoutButton)).perform(click())
    }
}
