package com.itantra.app.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Short.toHexString(): String {
    return String.format("0x%04X", this.toInt() and 0xFFFF)
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}
