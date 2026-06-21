package com.btsheng.erp.page

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.btsheng.erp.feature.MainActivity

/**
 * 登录页 Page Object（Compose · testTag）
 */
object LoginPage {

    @Volatile
    var composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>? = null

    fun loginAs(username: String, password: String) {
        val rule = composeRule ?: error("请在测试中设置 LoginPage.composeRule = createAndroidComposeRule<MainActivity>()")
        rule.onNodeWithTag("username").performTextInput(username)
        rule.onNodeWithTag("password").performTextInput(password)
        rule.onNodeWithTag("loginButton").performClick()
        rule.waitForIdle()
    }

    fun quickLoginAsOperator() {
        val rule = composeRule ?: error("请在测试中设置 LoginPage.composeRule")
        rule.onNodeWithText("操作工").performClick()
        rule.waitForIdle()
    }

    fun assertOnLoginPage() {
        val rule = composeRule ?: error("请在测试中设置 LoginPage.composeRule")
        rule.onNodeWithTag("loginButton").assertExists()
    }
}
