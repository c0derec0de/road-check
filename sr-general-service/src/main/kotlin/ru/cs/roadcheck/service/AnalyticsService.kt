package ru.cs.roadcheck.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.cs.roadcheck.repository.DangerousZoneRepository
import ru.cs.roadcheck.rest.dto.Coordinates
import ru.cs.roadcheck.rest.dto.DangerousZoneResponse
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.rest.dto.DangerousZonesListResponse

private val logger = KotlinLogging.logger {}

@Service
class AnalyticsService(
    private val dangerousZoneRepository: DangerousZoneRepository,
    private val reportRepository: ReportRepository,
) {

    fun getDangerousZones(regionId: Long?): DangerousZonesListResponse {
        logger.debug { "Getting dangerous zones for regionId=$regionId" }
        val zones = dangerousZoneRepository.findActiveByRegion(regionId)
        logger.debug { "Found ${zones.size} active dangerous zones" }
        val zonesResponse = zones.map { zone ->
            DangerousZoneResponse(
                id = zone.id!!,
                name = zone.name,
                incidents = zone.incidentsCount,
                riskLevel = zone.riskLevel,
                coordinates = Coordinates(
                    lat = zone.centerLat,
                    lng = zone.centerLng,
                ),
            )
        }
        logger.info { "Returning ${zonesResponse.size} dangerous zones" }
        return DangerousZonesListResponse(
            zones = zonesResponse,
            total = zonesResponse.size,
        )
    }

    fun getDangerousZonesForUser(userId: Long, regionId: Long?): DangerousZonesListResponse {
        val allowedRegions = reportRepository.findDistinctRegionIdsByUserId(userId).toMutableSet()
        if (allowedRegions.isEmpty()) {
            return DangerousZonesListResponse(zones = emptyList(), total = 0)
        }
        when {
            regionId == null -> { }
            regionId in allowedRegions -> {
                allowedRegions.clear()
                allowedRegions.add(regionId)
            }
            else -> return DangerousZonesListResponse(zones = emptyList(), total = 0)
        }
        val zones = dangerousZoneRepository.findActiveByRegionIdIn(allowedRegions)
        val zonesResponse = zones.map { zone ->
            DangerousZoneResponse(
                id = zone.id!!,
                name = zone.name,
                incidents = zone.incidentsCount,
                riskLevel = zone.riskLevel,
                coordinates = Coordinates(
                    lat = zone.centerLat,
                    lng = zone.centerLng,
                ),
            )
        }
        return DangerousZonesListResponse(
            zones = zonesResponse,
            total = zonesResponse.size,
        )
    }
}
