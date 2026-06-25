package ru.cs.roadcheck.rest.dto.manager

import io.swagger.v3.oas.annotations.media.Schema
import ru.cs.roadcheck.common.domain.entities.Report
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.rest.dto.CreateReportRequest
import ru.cs.roadcheck.common.domain.entities.User
import ru.cs.roadcheck.rest.dto.LocationResponse
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.ReportDetailResponse
import ru.cs.roadcheck.rest.dto.ReportListItemResponse
import ru.cs.roadcheck.rest.dto.UserInfoResponse
import java.math.BigDecimal

@Schema(description = "Запрос создания/обновления отчёта (менеджер)")
data class ReportManagerRequest(
    val policeUserId: Long,
    val userId: Long,
    val incidentType: String,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val description: String?,
    val photosUuid: String?,
    val status: String?,
    val fatalities: Int?,
    val injuries: Int?,
    val cause: String?,
    val riskLevel: String?,
    val title: String?,
    val address: String?,
    val isDangerousZone: Boolean?,
)

fun CreateReportRequest.toReport(): Report {
    val req = this
    return Report().apply {
        policeUserId = req.policeUserId
        userId = req.userId
        incidentType = req.incidentType.trim()
        latitude = req.latitude
        longitude = req.longitude
        description = req.description
        comment = null
        photosUuid = req.photosUuid
        status = ReportStatus.NEW
        fatalities = req.fatalities
        injuries = req.injuries
        cause = req.cause
    }
}

fun ReportManagerRequest.toReport(): Report {
    val req = this
    return Report().apply {
        policeUserId = req.policeUserId
        userId = req.userId
        incidentType = req.incidentType.trim()
        latitude = req.latitude
        longitude = req.longitude
        description = req.description
        comment = null
        photosUuid = req.photosUuid
        status = req.status?.trim()?.uppercase()?.let { ReportStatus.valueOf(it) } ?: ReportStatus.NEW
        fatalities = req.fatalities
        injuries = req.injuries
        cause = req.cause
        riskLevel = req.riskLevel
        title = req.title
        address = req.address
        isDangerousZone = req.isDangerousZone
    }
}

fun Report.toReportListItemResponse(username: String?, date: String?) = ReportListItemResponse(
    id = id!!,
    title = title ?: address,
    address = address,
    description = description,
    status = status?.name,
    riskLevel = riskLevel,
    isDangerousZone = isDangerousZone ?: false,
    username = username,
    date = date,
)

fun Report.toReportDetailResponse(user: User?) = ReportDetailResponse(
    id = id!!,
    title = title ?: address,
    address = address,
    description = description,
    comment = comment,
    status = status?.name,
    riskLevel = riskLevel,
    isDangerousZone = isDangerousZone ?: false,
    user = user?.let {
        UserInfoResponse(
            id = it.id!!,
            username = it.login ?: it.vkId ?: it.email ?: it.phone,
            fullName = listOfNotNull(it.firstname, it.middlename, it.lastname).joinToString(" ").takeIf { n -> n.isNotBlank() },
            phone = it.phone,
            blockchainVerified = it.blockchainVerified ?: false,
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
    photos = photosUuid?.split(",")?.map { it.trim() } ?: emptyList(),
    location = if (latitude != null && longitude != null) LocationResponse(lat = latitude, lng = longitude) else null,
    blockchainTxHash = blockchainTxHash,
    blockchainVerified = blockchainVerified ?: (blockchainTxHash != null),
    blockchainBlockNumber = blockchainBlockNumber,
    comments = emptyList(),
)

fun Report.toReportResponse() = ReportResponse(
    id = id!!,
    policeUserId = policeUserId!!,
    userId = userId,
    incidentType = incidentType!!,
    latitude = latitude,
    longitude = longitude,
    description = description,
    comment = comment,
    photosUuid = photosUuid,
    status = status?.name,
    createdAt = createdAt,
    blockchainTxHash = blockchainTxHash,
    fatalities = fatalities,
    injuries = injuries,
    cause = cause,
)
