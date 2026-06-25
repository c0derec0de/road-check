package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Метрики дашборда")
data class DashboardMetricsResponse(
    @Schema(description = "Общее количество инцидентов")
    val totalIncidents: Long,

    @Schema(description = "Процент изменения числа инцидентов за неделю (например: +12.5 или -5.2)")
    val incidentsChange: String,

    @Schema(description = "Количество активных пользователей")
    val activeUsers: Long,

    @Schema(description = "Процент изменения числа пользователей за неделю")
    val usersChange: String,

    @Schema(description = "Рост безопасности (средний уровень риска)")
    val safetyGrowth: Double,

    @Schema(description = "Изменение уровня безопасности за неделю")
    val safetyChange: String,

    @Schema(description = "Количество опасных зон")
    val dangerousZones: Long,

    @Schema(description = "Изменение числа опасных зон за неделю")
    val zonesChange: String,
)
