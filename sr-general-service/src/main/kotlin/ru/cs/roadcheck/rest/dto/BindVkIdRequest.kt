package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Запрос привязки VK ID")
data class BindVkIdRequest(
    @Schema(description = "Идентификатор пользователя VK", requiredMode = Schema.RequiredMode.REQUIRED, example = "id123456789")
    val vkId: String,
)
