package com.itantra.app.domain.model

enum class DeliveryStatus {
    DRAFT,
    PROCESSING,
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    PENDING,
    EXPIRED
}
