package ru.cs.roadcheck.rest.dto.manager

import io.swagger.v3.oas.annotations.media.Schema
import ru.cs.roadcheck.common.domain.entities.DangerousZone
import java.math.BigDecimal

@Schema(description = "Запрос создания/обновления опасной зоны")
data class DangerousZoneManagerRequest(
    val name: String?,
    val centerLat: BigDecimal?,
    val centerLng: BigDecimal?,
    val radius: Int?,
    val incidentsCount: Int?,
    val riskLevel: String?,
    val isActive: Boolean?,
    val regionId: Long?,
)

@Schema(description = "Ответ опасной зоны")
data class DangerousZoneManagerResponse(
    val id: Long,
    val name: String?,
    val centerLat: BigDecimal?,
    val centerLng: BigDecimal?,
    val radius: Int?,
    val incidentsCount: Int?,
    val riskLevel: String?,
    val isActive: Boolean?,
    val regionId: Long?,
)

fun DangerousZone.toDangerousZoneManagerResponse() = DangerousZoneManagerResponse(
    id = id!!,
    name = name,
    centerLat = centerLat,
    centerLng = centerLng,
    radius = radius,
    incidentsCount = incidentsCount,
    riskLevel = riskLevel,
    isActive = isActive ?: true,
    regionId = regionId,
)

fun DangerousZoneManagerRequest.toDangerousZone(): DangerousZone {
    val req = this
    return DangerousZone().apply {
        name = req.name
        centerLat = req.centerLat
        centerLng = req.centerLng
        radius = req.radius
        incidentsCount = req.incidentsCount
        riskLevel = req.riskLevel
        isActive = req.isActive ?: true
        regionId = req.regionId
    }
}
