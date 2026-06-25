package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Тело запроса авторизации")
data class LoginRequest(
    @Schema(description = "Идентификатор входа (login, email, vkId или phone)", requiredMode = Schema.RequiredMode.REQUIRED, example = "user")
    val login: String,

    @Schema(description = "Пароль", requiredMode = Schema.RequiredMode.REQUIRED, example = "secret")
    val password: String,
)
