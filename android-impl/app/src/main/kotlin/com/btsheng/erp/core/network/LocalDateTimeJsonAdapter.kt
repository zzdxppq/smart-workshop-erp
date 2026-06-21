package com.btsheng.erp.core.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Moshi · ISO-8601 LocalDateTime（与 Spring Boot Jackson 默认格式对齐） */
object LocalDateTimeJsonAdapter {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @ToJson
    fun toJson(value: LocalDateTime?): String? = value?.format(formatter)

    @FromJson
    fun fromJson(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return LocalDateTime.parse(value, formatter)
    }
}
