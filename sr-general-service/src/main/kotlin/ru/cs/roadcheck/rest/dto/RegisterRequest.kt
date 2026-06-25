package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Регистрация пользователя")
data class RegisterRequest(
    @Schema(description = "Уникальный логин пользователя", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
    val login: String,

    @Schema(description = "Пароль", example = "secret123", requiredMode = Schema.RequiredMode.REQUIRED)
    val password: String,

    @Schema(description = "Email", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,

    @Schema(description = "Имя")
    val firstname: String? = null,

    @Schema(description = "Фамилия")
    val lastname: String? = null,

    @Schema(description = "Телефон", requiredMode = Schema.RequiredMode.REQUIRED)
    val phone: String,

    @Schema(description = "Адрес кошелька")
    val walletAddress: String? = null,
)
