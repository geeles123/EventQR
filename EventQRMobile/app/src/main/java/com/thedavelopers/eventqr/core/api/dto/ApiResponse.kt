package com.thedavelopers.eventqr.core.api.dto

import java.time.Instant

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val timestamp: Instant? = null,
)
