package com.btsheng.erp.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.btsheng.erp.feature.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec C.1 · 操作工奶奶测试：30 秒内完成扫码三码报工 */
@RunWith(AndroidJUnit4::class)
class GrandmaScanE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun grandma_three_code_report_within_30_seconds() {
        val start = System.currentTimeMillis()

        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("loginButton").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("操作工").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("昆山佰泰胜 ERP").fetchSemanticsNodes().isNotEmpty()
            }
        }

        composeRule.onNodeWithText("手动输入 / 三码流程").performClick()
        composeRule.onNodeWithText("扫码三码").assertExists()

        composeRule.onNodeWithTag("scan_manual_input").performTextInput("GD-20260615-0001")
        composeRule.onNodeWithTag("scan_next_button").performClick()

        composeRule.onNodeWithTag("scan_manual_input").performTextInput("LZ-GD001-P01")
        composeRule.onNodeWithTag("scan_next_button").performClick()

        composeRule.onNodeWithTag("scan_manual_input").performTextInput("SB-CNC-001")
        composeRule.onNodeWithTag("scan_next_button").performClick()

        assertTrue("奶奶测试应在 30 秒内完成", System.currentTimeMillis() - start < 30_000)
    }
}
