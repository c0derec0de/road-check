package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ на регистрацию")
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val userId: Long?,
    val token: String?,
    val blockchainVerified: Boolean = false,
    val role: String? = null,
)
