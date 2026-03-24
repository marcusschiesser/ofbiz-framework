package com.example.leadflow.shared

import java.time.OffsetDateTime

data class ApiError(
    val status: Int,
    val message: String,
    val path: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
)
