package com.btsheng.erp.feature.v139

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.btsheng.erp.R

/** E12.1 · 从扫码页跳转图纸预览 */
class DrawingPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing_preview)
        if (savedInstanceState == null) {
            val workorderNo = intent.getStringExtra(EXTRA_WORKORDER_NO).orEmpty()
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.drawingPreviewContainer,
                    OperatorDrawingPreviewFragment.newInstance(
                        drawingId = intent.getLongExtra(EXTRA_DRAWING_ID, 1L),
                        processId = intent.getLongExtra(EXTRA_PROCESS_ID, 1L),
                        workorderNo = workorderNo,
                        processNo = intent.getStringExtra(EXTRA_PROCESS_NO) ?: "P01",
                    ),
                )
                .commit()
        }
    }

    companion object {
        const val EXTRA_WORKORDER_NO = "workorderNo"
        const val EXTRA_DRAWING_ID = "drawingId"
        const val EXTRA_PROCESS_ID = "processId"
        const val EXTRA_PROCESS_NO = "processNo"

        fun launch(context: Context, workorderNo: String) {
            context.startActivity(
                Intent(context, DrawingPreviewActivity::class.java).apply {
                    putExtra(EXTRA_WORKORDER_NO, workorderNo)
                    putExtra(EXTRA_DRAWING_ID, 1L)
                    putExtra(EXTRA_PROCESS_ID, 1L)
                    putExtra(EXTRA_PROCESS_NO, "P01")
                },
            )
        }
    }
}
