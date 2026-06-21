package com.btsheng.erp.feature.v139

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * PDF Print Adapter · android-impl 打印适配器
 *
 * <p>将后端返回的 PDF 文件转为 Printable · 调 Android PrintManager
 * <p>从 PDF 文件读取字节 → 写入 spool 文件 → onWrite 完成
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
class PdfPrintAdapter(
    private val context: Context,
    private val pdfFile: File
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("labels.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(pdfFile).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        }
    }
}
