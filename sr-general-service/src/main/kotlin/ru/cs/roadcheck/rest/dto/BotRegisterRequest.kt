package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос на регистрацию пользователя ботом")
data class BotRegisterRequest(
    @Schema(description = "Логин пользователя", example = "bot_user_123")
    val login: String,

    @Schema(description = "Пароль пользователя", example = "password123")
    val password: String,

    @Schema(description = "Email пользователя", example = "user@example.com")
    val email: String,

    @Schema(description = "VK ID пользователя", example = "12345678")
    val vkId: String,

    @Schema(description = "Имя пользователя", example = "Иван")
    val firstname: String? = null,

    @Schema(description = "Фамилия пользователя", example = "Иванов")
    val lastname: String? = null,

    @Schema(description = "Телефон пользователя", example = "+79991234567")
    val phone: String,

    @Schema(description = "Адрес кошелька", example = "0x1234567890abcdef")
    val walletAddress: String? = null,
)
