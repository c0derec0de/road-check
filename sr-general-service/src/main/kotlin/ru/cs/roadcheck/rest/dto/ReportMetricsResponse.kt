package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Метрики по отчётам")
data class ReportMetricsResponse(
    @Schema(description = "Общее количество отчётов")
    val total: Long,

    @Schema(description = "Процент изменения общего количества")
    val totalChange: String,

    @Schema(description = "Количество новых отчётов")
    val new: Long,

    @Schema(description = "Процент изменения новых отчётов")
    val newChange: String,

    @Schema(description = "Количество отчётов в работе")
    val inProgress: Long,

    @Schema(description = "Процент изменения отчётов в работе")
    val inProgressChange: String,

    @Schema(description = "Количество завершённых отчётов")
    val completed: Long,

    @Schema(description = "Процент изменения завершённых отчётов")
    val completedChange: String,
)
