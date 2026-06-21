package com.btsheng.erp.feature.v138

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** V1.3.8 · 分批到货 Activity */
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BatchIncomingScanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = BatchIncomingScanFragment().apply {
                arguments = Bundle().apply {
                    intent.getLongExtra(EXTRA_PO_ID, 0L).takeIf { it > 0L }?.let { putLong(BatchIncomingScanFragment.ARG_PO_ID, it) }
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_PO_ID = "po_id"

        fun intent(context: Context, poId: Long? = null): Intent =
            Intent(context, BatchIncomingScanActivity::class.java).apply {
                poId?.let { putExtra(EXTRA_PO_ID, it) }
            }
    }
}
