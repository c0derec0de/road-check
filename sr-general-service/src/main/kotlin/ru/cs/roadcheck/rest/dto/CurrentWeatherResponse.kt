package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Текущая погода и уровень риска")
data class CurrentWeatherResponse(
    @Schema(description = "Информация о погоде")
    val weather: WeatherInfo,

    @Schema(description = "Уровень риска")
    val riskLevel: String?,

    @Schema(description = "Время последнего обновления (формат: HH:mm)")
    val lastUpdated: String?,
)

@Schema(description = "Информация о погоде")
data class WeatherInfo(
    @Schema(description = "Условия погоды")
    val condition: String?,

    @Schema(description = "Температура")
    val temperature: Int?,

    @Schema(description = "Единица измерения")
    val unit: String,

    @Schema(description = "Скорость ветра, км/ч")
    val windSpeedKmh: BigDecimal?,

    @Schema(description = "Влажность воздуха, %")
    val humidity: Int?,
)
