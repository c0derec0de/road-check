package ru.cs.roadcheck.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Детальная информация об отчёте")
data class ReportDetailResponse(
    @Schema(description = "ID отчёта")
    val id: Long,

    @Schema(description = "Заголовок")
    val title: String?,

    @Schema(description = "Адрес")
    val address: String?,

    @Schema(description = "Описание")
    val description: String?,

    @Schema(description = "Комментарий модератора")
    val comment: String?,

    @Schema(description = "Статус")
    val status: String?,

    @Schema(description = "Уровень риска")
    val riskLevel: String?,

    @Schema(description = "Является ли опасной зоной")
    val isDangerousZone: Boolean?,

    @Schema(description = "Информация о пользователе")
    val user: UserInfoResponse?,

    @Schema(description = "Дата создания")
    val createdAt: Instant?,

    @Schema(description = "Дата обновления")
    val updatedAt: Instant?,

    @Schema(description = "Список URL фотографий")
    val photos: List<String>,

    @Schema(description = "Местоположение")
    val location: LocationResponse?,

    @Schema(description = "Хэш транзакции в блокчейне")
    val blockchainTxHash: String?,

    @Schema(description = "Подтверждено ли в блокчейне")
    val blockchainVerified: Boolean?,

    @Schema(description = "Номер блока в блокчейне")
    val blockchainBlockNumber: Long?,

    @Schema(description = "Комментарии")
    val comments: List<CommentResponse>,
)

@Schema(description = "Информация о пользователе")
data class UserInfoResponse(
    @Schema(description = "ID пользователя")
    val id: Long,

    @Schema(description = "Имя пользователя (telegram)")
    val username: String?,

    @Schema(description = "Полное имя")
    val fullName: String?,

    @Schema(description = "Телефон")
    val phone: String?,

    @Schema(description = "Подтверждён ли в блокчейне")
    val blockchainVerified: Boolean?,
)

@Schema(description = "Местоположение")
data class LocationResponse(
    @Schema(description = "Широта")
    val lat: BigDecimal?,

    @Schema(description = "Долгота")
    val lng: BigDecimal?,
)

@Schema(description = "Комментарий")
data class CommentResponse(
    @Schema(description = "ID комментария")
    val id: Long,

    @Schema(description = "Имя пользователя")
    val user: String?,

    @Schema(description = "Текст комментария")
    val text: String?,

    @Schema(description = "Дата создания")
    val createdAt: Instant?,
)
