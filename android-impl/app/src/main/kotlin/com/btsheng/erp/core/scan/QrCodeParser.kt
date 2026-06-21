package com.btsheng.erp.core.scan

import java.util.regex.Pattern

/** 5 类码 prefix（Spec · GD-/LZ-/SB-/WL-/WW-） */
object QrCodeParser {

    const val TYPE_WORK_ORDER = "WORK_ORDER"
    const val TYPE_MATERIAL = "MATERIAL"
    const val TYPE_FLOW = "FLOW"
    const val TYPE_DEVICE = "DEVICE"
    const val TYPE_OUTSOURCE_ORDER = "OUTSOURCE_ORDER"
    const val TYPE_UNKNOWN = "UNKNOWN"
    const val MANUAL_INPUT = "manual-input"

    private val GD = Pattern.compile("^GD-(\\d{8})-(\\d{4})$")
    private val WL = Pattern.compile("^WL-([A-Z0-9-]+)$")
    private val LZ = Pattern.compile("^LZ-([A-Z0-9-]+)$")
    private val SB = Pattern.compile("^SB-([A-Z0-9-]+)$")
    private val WW = Pattern.compile("^WW-?(\\d{8})-(\\d{4})$")

    data class ParseResult(
        val type: String,
        val id: Long?,
        val code: String?,
        val routeUrl: String,
        val ok: Boolean
    ) {
        companion object {
            fun unknown() = ParseResult(TYPE_UNKNOWN, null, null, MANUAL_INPUT, false)
        }
    }

    fun parse(code: String?): ParseResult {
        if (code.isNullOrBlank()) return ParseResult.unknown()
        val trimmed = code.trim()
        fun m(p: Pattern) = p.matcher(trimmed)

        m(GD).takeIf { it.matches() }?.let {
            return ParseResult(TYPE_WORK_ORDER, it.group(2).toLong(), trimmed, "workorder/detail/${it.group(2)}", true)
        }
        m(WL).takeIf { it.matches() }?.let {
            return ParseResult(TYPE_MATERIAL, null, trimmed, "material/detail/${it.group(1)}", true)
        }
        m(LZ).takeIf { it.matches() }?.let {
            return ParseResult(TYPE_FLOW, null, trimmed, "flow/detail/${it.group(1)}", true)
        }
        m(SB).takeIf { it.matches() }?.let {
            return ParseResult(TYPE_DEVICE, null, trimmed, "device/detail/${it.group(1)}", true)
        }
        m(WW).takeIf { it.matches() }?.let {
            val normalized = "WW${it.group(1)}-${it.group(2)}"
            return ParseResult(TYPE_OUTSOURCE_ORDER, it.group(2).toLong(), normalized, "outsource/detail/${it.group(2)}", true)
        }
        return ParseResult.unknown()
    }
}
