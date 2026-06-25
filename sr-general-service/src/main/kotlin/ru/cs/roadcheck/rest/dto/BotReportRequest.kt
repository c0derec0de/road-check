package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Запрос на создание отчёта ботом по VK ID")
data class BotReportRequest(
    @Schema(description = "VK ID пользователя", example = "12345678")
    val vkId: String,

    @Schema(description = "Тип инцидента", example = "Повреждение дорожного покрытия")
    val incidentType: String,

    @Schema(description = "Описание инцидента", example = "Большая яма на проезжей части")
    val description: String,

    @Schema(description = "Широта места инцидента", example = "55.755826")
    val latitude: BigDecimal,

    @Schema(description = "Долгота места инцидента", example = "37.617299")
    val longitude: BigDecimal,

    @Schema(description = "ID региона", example = "1")
    val regionId: Long? = null,

    @Schema(description = "URL фотографии", example = "https://example.com/photo.jpg")
    val photoUrl: String? = null,
)
