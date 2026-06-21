package com.btsheng.erp.util

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * V1.3.9 Sprint 14 Story 13.6 · ADB 工具（android-impl E2E）
 *
 * <p>扫码模拟：通过 ZXing intent 注入 barcode 内容，模拟真机扫码行为。
 * <p>网络模拟：调 UiDevice.executeShellCommand 切 WiFi 状态。
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object AdbUtils {

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /**
     * 注入 ZXing 扫码结果（模拟真机扫码）
     *
     * <p>实际生产：通过 {@code am broadcast -a com.google.zxing.client.android.SCAN}
     * 触发 ScanActivity → 注入 RESULT intent。
     */
    fun injectScanResult(barcode: String) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = android.content.Intent("com.google.zxing.client.android.SCAN").apply {
            putExtra("SCAN_RESULT", barcode)
            putExtra("SCAN_RESULT_FORMAT", "QR_CODE")
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(intent)
    }

    /**
     * 模拟网络断开（断 WiFi）· 用于异常路径测试
     */
    fun simulateNetworkDown() {
        device.executeShellCommand("svc wifi disable")
        device.executeShellCommand("svc data disable")
    }

    /**
     * 恢复网络（开 WiFi）· 用于异常路径测试
     */
    fun simulateNetworkUp() {
        device.executeShellCommand("svc wifi enable")
        device.executeShellCommand("svc data enable")
    }

    /**
     * 截屏保存到 /sdcard/Pictures（用于 E2E 调试）
     */
    fun takeScreenshot(name: String) {
        val path = "/sdcard/Pictures/$name.png"
        device.executeShellCommand("screencap -p $path")
    }
}
