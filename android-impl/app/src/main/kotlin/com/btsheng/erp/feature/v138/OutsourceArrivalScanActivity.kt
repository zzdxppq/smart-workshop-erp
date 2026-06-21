package com.btsheng.erp.feature.v138

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/** V1.3.5 · E12-S2 · 委外到货扫码 Activity */
@AndroidEntryPoint
class OutsourceArrivalScanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = OutsourceArrivalScanFragment().apply {
                arguments = Bundle().apply {
                    intent.getStringExtra(EXTRA_OUTSOURCE_NO)?.let {
                        putString(OutsourceArrivalScanFragment.ARG_OUTSOURCE_NO, it)
                    }
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_OUTSOURCE_NO = "outsource_no"

        fun intent(context: Context, outsourceNo: String? = null): Intent =
            Intent(context, OutsourceArrivalScanActivity::class.java).apply {
                outsourceNo?.let { putExtra(EXTRA_OUTSOURCE_NO, it) }
            }
    }
}
