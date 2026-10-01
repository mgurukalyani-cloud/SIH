package com.itantra.app.domain.model

enum class MessagePriority(val value: Int) {
    NORMAL(0),
    HIGH(1),
    EMERGENCY(3);

    companion object {
        fun fromValue(value: Int): MessagePriority {
            return entries.firstOrNull { it.value == value } ?: NORMAL
        }
    }
}
