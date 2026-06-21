package com.btsheng.erp.core.scan

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * 仓管 / 车间统一扫码入口（PRD · 扫码或手动输入二选一）
 */
object BarcodeScanHelper {

    fun options(prompt: String): ScanOptions = ScanOptions().apply {
        setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        setPrompt(prompt)
        setBeepEnabled(true)
        setOrientationLocked(false)
    }

    /** Fragment：注册 ZXing 扫码，结果非空时回调 */
    fun Fragment.registerBarcodeScan(onScanned: (String) -> Unit): ActivityResultLauncher<ScanOptions> =
        registerForActivityResult(ScanContract()) { result ->
            result.contents?.trim()?.takeIf { it.isNotEmpty() }?.let(onScanned)
        }
}
