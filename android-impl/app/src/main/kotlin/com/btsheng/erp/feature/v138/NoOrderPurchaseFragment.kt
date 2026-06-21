package com.btsheng.erp.feature.v138

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.btsheng.erp.R

/**
 * V1.3.8 Sprint 8 Story 8.5 · 无订单采购 Fragment（ViewBinding）
 *
 * 视图：{@code res/layout/fragment_v138_no_order_purchase.xml}
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
class NoOrderPurchaseFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_v138_no_order_purchase, container, false)
    }
}
