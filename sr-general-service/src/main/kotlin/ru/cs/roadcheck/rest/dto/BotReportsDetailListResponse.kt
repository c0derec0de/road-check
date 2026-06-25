package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Все неархивированные отчёты пользователя (полные карточки) для бота")
data class BotReportsDetailListResponse(
    @Schema(description = "Отчёты от новых к старым")
    val reports: List<ReportDetailResponse>,
    @Schema(description = "Количество отчётов в ответе")
    val total: Int,
)
