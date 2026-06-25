package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Тело запроса на создание отчёта")
data class CreateReportRequest(
    @Schema(description = "ID пользователя-сотрудника полиции", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    val policeUserId: Long,

    @Schema(description = "ID пользователя-заявителя", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    val userId: Long,

    @Schema(description = "Тип инцидента", requiredMode = Schema.RequiredMode.REQUIRED, example = "ДТП", maxLength = 100)
    val incidentType: String,

    @Schema(description = "Широта места происшествия", example = "55.755826")
    val latitude: BigDecimal?,

    @Schema(description = "Долгота места происшествия", example = "37.617299")
    val longitude: BigDecimal?,

    @Schema(description = "Текстовое описание инцидента")
    val description: String?,

    @Schema(description = "UUID фотографий (строка до 255 символов)", maxLength = 255)
    val photosUuid: String?,

    @Schema(description = "Количество погибших")
    val fatalities: Int?,

    @Schema(description = "Количество пострадавших")
    val injuries: Int?,

    @Schema(description = "Причина инцидента", maxLength = 255)
    val cause: String?,
)
