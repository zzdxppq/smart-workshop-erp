package com.btsheng.erp.feature.v138

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** V1.3.8 · 物料码扫码 Activity（入库/出库/解析） */
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MaterialBarcodeScanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = MaterialBarcodeScanFragment().apply {
                arguments = Bundle().apply {
                    putString(MaterialBarcodeScanFragment.ARG_MODE, intent.getStringExtra(EXTRA_MODE) ?: MaterialScanMode.PARSE_ONLY.name)
                    intent.getStringExtra(EXTRA_BARCODE)?.let { putString(MaterialBarcodeScanFragment.ARG_BARCODE, it) }
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_MODE = "scan_mode"
        const val EXTRA_BARCODE = "barcode"

        fun intent(context: Context, mode: MaterialScanMode, barcode: String? = null): Intent =
            Intent(context, MaterialBarcodeScanActivity::class.java).apply {
                putExtra(EXTRA_MODE, mode.name)
                barcode?.let { putExtra(EXTRA_BARCODE, it) }
            }
    }
}
