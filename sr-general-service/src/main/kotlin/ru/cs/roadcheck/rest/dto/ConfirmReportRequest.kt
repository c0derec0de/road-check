package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Подтверждение отчёта")
data class ConfirmReportRequest(
    @Schema(description = "Комментарий модератора", requiredMode = Schema.RequiredMode.REQUIRED, example = "Факт ДТП подтвержден")
    val comment: String,
)
