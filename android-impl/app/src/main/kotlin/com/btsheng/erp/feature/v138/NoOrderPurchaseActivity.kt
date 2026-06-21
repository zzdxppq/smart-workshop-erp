package com.btsheng.erp.feature.v138

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * V1.3.8 Sprint 8 Story 8.5 · 无订单采购 Activity
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
class NoOrderPurchaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, NoOrderPurchaseFragment())
                .commit()
        }
    }
}
