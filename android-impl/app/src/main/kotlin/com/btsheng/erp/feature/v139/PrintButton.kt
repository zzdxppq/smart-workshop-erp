package com.btsheng.erp.feature.v139

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.btsheng.erp.R
import com.btsheng.erp.feature.v138.ApiClient
import com.btsheng.erp.feature.v138.PrintPdfA4Item
import com.btsheng.erp.feature.v138.PrintPdfA4Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream

/**
 * V1.3.9 Sprint 12 Story 12.4 · android-impl 简化版 PrintButton (AC-12.4.5)
 *
 * <p>手机无标签打印机 · 仅支持 PDF_BROWSER 模式
 * <p>调 /print/labels/pdf-a4 → 拿 base64 → 调 Android PrintManager API
 * <p>集成到工单扫码 Fragment · "打印" 按钮触发
 *
 * @author dev agent Opus 4.8 · 2026-06-14
 */
class PrintButton {

    companion object {
        private const val TAG = "PrintButton"
    }

    /**
     * 触发打印 · 供宿主 Activity/Fragment 调用
     *
     * @param context Android Context
     * @param scope 用于协程的 LifecycleOwner
     * @param codeType 模板代号 GD/LZ/SB/WW/WL
     * @param codeValue 二维码内容
     * @param lines 文本行 · max 6
     */
    fun triggerPrint(
        context: Context,
        scope: androidx.lifecycle.LifecycleOwner,
        codeType: String,
        codeValue: String,
        lines: List<String> = emptyList()
    ) {
        Toast.makeText(context, "正在生成 PDF…", Toast.LENGTH_SHORT).show()
        scope.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = Retrofit.Builder()
                    .baseUrl("https://erp.yourcompany.local/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                    .create(ApiClient::class.java)
                val req = PrintPdfA4Request(
                    items = listOf(
                        PrintPdfA4Item(
                            templateCode = codeType,
                            qrContent = codeValue,
                            lines = lines
                        )
                    )
                )
                val resp = client.printPdfA4(req)
                Log.i(TAG, "PDF generated: logNo=${resp.logNo} bytes=${resp.bytes}")

                // base64 → 临时文件 → PrintManager 打印
                val pdfBytes = Base64.decode(resp.pdfBase64, Base64.DEFAULT)
                val pdfFile = File(context.cacheDir, "labels-${resp.logNo}.pdf")
                FileOutputStream(pdfFile).use { it.write(pdfBytes) }

                withContext(Dispatchers.Main) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Labels-${resp.logNo}"
                    printManager.print(jobName, PdfPrintAdapter(context, pdfFile), PrintAttributes.Builder().build())
                    Toast.makeText(context, "已生成 PDF · 选系统打印机", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "PDF print failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "打印失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
