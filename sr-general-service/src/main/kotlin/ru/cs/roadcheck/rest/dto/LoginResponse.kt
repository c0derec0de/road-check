package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ на запрос авторизации или выхода")
data class LoginResponse(
    @Schema(description = "Успешность операции")
    val success: Boolean,

    @Schema(description = "Сообщение для пользователя")
    val message: String,

    @Schema(description = "ID пользователя (при успешном login)")
    val userId: Long? = null,

    @Schema(description = "JWT токен (при успешном login)")
    val token: String? = null,

    @Schema(description = "Роль: USER или MODERATOR (при успешном login)")
    val role: String? = null,
)
