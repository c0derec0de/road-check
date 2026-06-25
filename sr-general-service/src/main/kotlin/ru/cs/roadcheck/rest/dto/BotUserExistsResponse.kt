package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Ответ проверки наличия пользователя по vkId")
data class BotUserExistsResponse(
    @Schema(description = "VK ID пользователя")
    val vkId: String,
    @Schema(description = "Найден ли пользователь")
    val exists: Boolean,
)
