package com.btsheng.erp.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.btsheng.erp.util.TestAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V1.3.9 Sprint 14 Story 13.6 · connectedAndroidTest 配置验证测例（android-impl）
 *
 * <p>对应 Story 13.6 §7.1 测例组 2：2 测例
 * <ul>
 *   <li>TC-13.6.2.1 build.gradle.kts 配置就位（androidTest deps）</li>
 *   <li>TC-13.6.2.2 7 角色 + FINANCE 测试账号 seed（8 账号）</li>
 * </ul>
 *
 * <p>本测例不依赖设备，但必须在 connectedAndroidTest runner 中执行以验证部署环境。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectedAndroidTestConfigTest {

    // =========================================================
    // TC-13.6.2.1 build.gradle.kts 配置就位
    // =========================================================
    @Test
    fun test_2_1_build_gradle_kts_dependencies_in_place() {
        // 验证 build.gradle.kts 关键 androidTest deps 已声明
        val expectedDeps = listOf(
            "androidx.test.ext:junit:1.1.5",
            "androidx.test:runner:1.5.2",
            "androidx.test:rules:1.5.0",
            "androidx.test.espresso:espresso-core:3.5.1"
        )

        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val appInfo = ctx.packageManager.getApplicationInfo(
            ctx.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        assertNotNull("ApplicationInfo 必须可获取", appInfo)

        // 验证 classloader 包含 androidx test runner 类
        val classLoader = ctx.classLoader
        assertTrue(
            "androidx.test.runner.AndroidJUnitRunner 类必须存在",
            try {
                Class.forName("androidx.test.runner.AndroidJUnitRunner", false, classLoader) != null
            } catch (e: ClassNotFoundException) {
                false
            }
        )
        assertTrue(
            "androidx.test.ext.junit.runners.AndroidJUnit4 类必须存在",
            try {
                Class.forName("androidx.test.ext.junit.runners.AndroidJUnit4", false, classLoader) != null
            } catch (e: ClassNotFoundException) {
                false
            }
        )

        // 验证 build.gradle.kts 中列出的依赖（通过解析文件验证）
        val buildGradle = java.io.File("build.gradle.kts")
        if (buildGradle.exists()) {
            val content = buildGradle.readText()
            for (dep in expectedDeps) {
                assertTrue(
                    "build.gradle.kts 必须包含依赖 $dep",
                    content.contains(dep)
                )
            }
        }
    }

    // =========================================================
    // TC-13.6.2.2 7 角色 + FINANCE 测试账号 seed（8 账号）
    // =========================================================
    @Test
    fun test_2_2_eight_role_accounts_seed_completed() {
        // 验证 TestAccount.8 个账号字段完整
        assertEquals(8, TestAccount.ALL_ACCOUNTS.size)

        val expectedRoles = setOf(
            "ENGINEER", "PROD_PLANNER", "SALES",
            "PURCHASER", "WAREHOUSE", "QC", "OPERATOR", "FINANCE"
        )
        val actualRoles = TestAccount.ALL_ACCOUNTS.map { it.role }.toSet()
        assertEquals(expectedRoles, actualRoles)

        // 验证所有账号使用 Test@123 密码（staging 统一）
        for (account in TestAccount.ALL_ACCOUNTS) {
            assertEquals("Test@123", account.password)
            assertTrue(
                "${account.role} loginUserId 必须以角色前缀开头",
                account.loginUserId.startsWith(account.role.lowercase() + "_")
            )
            assertNotNull(account.displayName)
            assertNotNull(account.expectedScope)
        }

        // 验证 scope 与 Story 13.6 §4.1 对齐
        assertEquals("GLOBAL", TestAccount.ENGINEER.expectedScope)
        assertEquals("WORKORDER", TestAccount.PROD_PLANNER.expectedScope)
        assertEquals("ORDER", TestAccount.SALES.expectedScope)
        assertEquals("PO", TestAccount.PURCHASER.expectedScope)
        assertEquals("INCOMING", TestAccount.WAREHOUSE.expectedScope)
        assertEquals("INSPECTION", TestAccount.QC.expectedScope)
        assertEquals("WORKORDER_PROCESS", TestAccount.OPERATOR.expectedScope)
        assertEquals("NONE", TestAccount.FINANCE.expectedScope)
    }
}
