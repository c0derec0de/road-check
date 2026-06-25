package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Погода в регионе сейчас")
data class RegionWeatherResponse(
    @Schema(description = "ID региона")
    val regionId: Long,

    @Schema(description = "Название региона")
    val regionName: String?,

    @Schema(description = "Температура, °C")
    val temperature: Int?,

    @Schema(description = "Осадки за последний период, мм (если поддерживается API)")
    val precipitationMm: BigDecimal?,

    @Schema(description = "Скорость ветра, км/ч")
    val windSpeedKmh: BigDecimal?,

    @Schema(description = "Влажность воздуха, %")
    val humidity: Int?,
)
