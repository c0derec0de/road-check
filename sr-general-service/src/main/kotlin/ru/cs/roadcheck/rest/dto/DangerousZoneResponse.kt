package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Опасная зона")
data class DangerousZoneResponse(
    @Schema(description = "ID зоны")
    val id: Long,

    @Schema(description = "Название зоны")
    val name: String?,

    @Schema(description = "Количество инцидентов в зоне")
    val incidents: Int?,

    @Schema(description = "Уровень риска (high, medium, low)")
    val riskLevel: String?,

    @Schema(description = "Координаты зоны")
    val coordinates: Coordinates,
)

@Schema(description = "Координаты")
data class Coordinates(
    @Schema(description = "Широта")
    val lat: BigDecimal?,

    @Schema(description = "Долгота")
    val lng: BigDecimal?,
)

@Schema(description = "Список опасных зон")
data class DangerousZonesListResponse(
    @Schema(description = "Список зон")
    val zones: List<DangerousZoneResponse>,

    @Schema(description = "Общее количество зон")
    val total: Int,
)
