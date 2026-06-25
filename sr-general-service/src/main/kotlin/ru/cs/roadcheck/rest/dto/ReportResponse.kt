package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Данные отчёта в ответе API")
data class ReportResponse(
    @Schema(description = "Уникальный идентификатор отчёта")
    val id: Long,

    @Schema(description = "ID сотрудника полиции")
    val policeUserId: Long,

    @Schema(description = "ID пользователя-заявителя (null после удаления пользователя и архивирования)")
    val userId: Long?,

    @Schema(description = "Тип инцидента")
    val incidentType: String,

    @Schema(description = "Широта")
    val latitude: BigDecimal?,

    @Schema(description = "Долгота")
    val longitude: BigDecimal?,

    @Schema(description = "Описание инцидента")
    val description: String?,

    @Schema(description = "Комментарий модератора")
    val comment: String?,

    @Schema(description = "UUID фотографий")
    val photosUuid: String?,

    @Schema(description = "Текущий статус отчёта (например: new, in_progress, closed)")
    val status: String?,

    @Schema(description = "Дата и время создания (ISO-8601)")
    val createdAt: Instant?,

    @Schema(description = "Хэш транзакции в блокчейне")
    val blockchainTxHash: String?,

    @Schema(description = "Количество погибших")
    val fatalities: Int?,

    @Schema(description = "Количество пострадавших")
    val injuries: Int?,

    @Schema(description = "Причина инцидента")
    val cause: String?,
)
