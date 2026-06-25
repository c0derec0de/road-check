package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Элемент списка отчётов")
data class ReportListItemResponse(
    @Schema(description = "ID отчёта")
    val id: Long,

    @Schema(description = "Заголовок отчёта")
    val title: String?,

    @Schema(description = "Адрес")
    val address: String?,

    @Schema(description = "Описание")
    val description: String?,

    @Schema(description = "Статус")
    val status: String?,

    @Schema(description = "Уровень риска")
    val riskLevel: String?,

    @Schema(description = "Является ли опасной зоной")
    val isDangerousZone: Boolean?,

    @Schema(description = "Имя пользователя (telegram)")
    val username: String?,

    @Schema(description = "Дата создания (формат: dd.MM.yyyy)")
    val date: String?,
)

@Schema(description = "Пагинация")
data class PaginationResponse(
    @Schema(description = "Текущая страница")
    val currentPage: Int,

    @Schema(description = "Всего страниц")
    val totalPages: Int,

    @Schema(description = "Всего элементов")
    val totalItems: Long,

    @Schema(description = "Элементов на странице")
    val itemsPerPage: Int,
)

@Schema(description = "Фильтры")
data class FiltersResponse(
    @Schema(description = "Доступные статусы")
    val availableStatuses: List<String>,

    @Schema(description = "Доступные уровни риска")
    val availableRiskLevels: List<String>,
)

@Schema(description = "Список отчётов с пагинацией")
data class ReportsListResponse(
    @Schema(description = "Список отчётов")
    val reports: List<ReportListItemResponse>,

    @Schema(description = "Информация о пагинации")
    val pagination: PaginationResponse,

    @Schema(description = "Доступные фильтры")
    val filters: FiltersResponse,
)
