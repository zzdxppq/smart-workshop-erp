package com.btsheng.erp.feature.v139

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.btsheng.erp.R

/** OPERATOR 灰度 · 工单工序 + 图纸入口 */
class WorkorderProcessScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing_preview)
        if (savedInstanceState == null) {
            val workorderNo = intent.getStringExtra(EXTRA_WORKORDER_NO).orEmpty()
            supportFragmentManager.beginTransaction()
                .replace(R.id.drawingPreviewContainer, WorkorderProcessScanFragment.newInstance(workorderNo))
                .commit()
        }
    }

    companion object {
        const val EXTRA_WORKORDER_NO = "workorderNo"

        fun launch(context: Context, workorderNo: String) {
            context.startActivity(
                Intent(context, WorkorderProcessScanActivity::class.java).apply {
                    putExtra(EXTRA_WORKORDER_NO, workorderNo)
                },
            )
        }
    }
}
