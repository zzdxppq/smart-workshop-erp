package com.btsheng.erp.feature.scan

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btsheng.erp.core.scan.QrCodeParser

@Preview(showBackground = true, name = "ScanStep0")
@Composable
fun PreviewScanScreenStep0() {
    ScanScreen()
}

@Preview(showBackground = true, name = "OfflineBarOnline")
@Composable
fun PreviewOfflineBarOnline() {
    Surface { OfflineStatusBar(pending = 0, online = true) }
}

@Preview(showBackground = true, name = "OfflineBarQueue")
@Composable
fun PreviewOfflineBarQueue() {
    Surface { OfflineStatusBar(pending = 5, online = true) }
}

@Preview(showBackground = true, name = "ParseResult")
@Composable
fun PreviewParseUnknown() {
    Surface(Modifier.padding(16.dp)) {
        Text(QrCodeParser.parse("GD-20260615-0001").routeUrl)
    }
}
