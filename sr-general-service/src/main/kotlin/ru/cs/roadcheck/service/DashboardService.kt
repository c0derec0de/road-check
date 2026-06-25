package ru.cs.roadcheck.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.cs.roadcheck.repository.DangerousZoneRepository
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.repository.RiskPredictionRepository
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.DashboardMetricsResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

@Service
class DashboardService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val dangerousZoneRepository: DangerousZoneRepository,
    private val riskPredictionRepository: RiskPredictionRepository,
) {

    fun getMetrics(): DashboardMetricsResponse {
        logger.debug { "Calculating dashboard metrics" }
        val now = Instant.now()
        val weekAgo = now.minus(7, ChronoUnit.DAYS)
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)

        val totalIncidents = reportRepository.count()
        logger.debug { "Total incidents: $totalIncidents" }
        val incidentsThisWeek = reportRepository.countByCreatedAtAfter(weekAgo)
        val incidentsLastWeek = reportRepository.countByCreatedAtBetween(twoWeeksAgo, weekAgo)
        val incidentsChange = calculateChangePercent(incidentsThisWeek, incidentsLastWeek)

        val activeUsers = userRepository.count()
        logger.debug { "Active users: $activeUsers" }
        val usersThisWeek = userRepository.countByCreatedAtAfter(weekAgo)
        val usersLastWeek = userRepository.countByCreatedAtBetween(twoWeeksAgo, weekAgo)
        val usersChange = calculateChangePercent(usersThisWeek, usersLastWeek)

        val avgRiskScore = riskPredictionRepository.findAverageRiskScoreSince(weekAgo)
            ?: BigDecimal.ZERO
        val safetyGrowth = avgRiskScore.toDouble()
        logger.debug { "Average risk score: $avgRiskScore" }
        val avgRiskScoreLastWeek = riskPredictionRepository.findAverageRiskScoreSince(twoWeeksAgo)
            ?: BigDecimal.ZERO
        val safetyChange = calculateChangePercent(avgRiskScore.toDouble(), avgRiskScoreLastWeek.toDouble())

        val dangerousZones = dangerousZoneRepository.countActive()
        logger.debug { "Dangerous zones: $dangerousZones" }
        val zonesThisWeek = dangerousZoneRepository.countActiveCreatedAtAfter(weekAgo)
        val zonesLastWeek = dangerousZoneRepository.countActiveCreatedAtBetween(twoWeeksAgo, weekAgo)
        val zonesChange = calculateChangePercent(zonesThisWeek, zonesLastWeek)

        logger.info { "Dashboard metrics calculated: incidents=$totalIncidents, users=$activeUsers, zones=$dangerousZones" }
        return DashboardMetricsResponse(
            totalIncidents = totalIncidents,
            incidentsChange = formatChange(incidentsChange),
            activeUsers = activeUsers,
            usersChange = formatChange(usersChange),
            safetyGrowth = safetyGrowth,
            safetyChange = formatChange(safetyChange),
            dangerousZones = dangerousZones,
            zonesChange = formatChange(zonesChange),
        )
    }

    fun getMetricsForUser(userId: Long): DashboardMetricsResponse {
        logger.debug { "Calculating dashboard metrics for userId=$userId" }
        val now = Instant.now()
        val weekAgo = now.minus(7, ChronoUnit.DAYS)
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)

        val totalIncidents = reportRepository.countByUserId(userId)
        val incidentsThisWeek = reportRepository.countByUserIdAndCreatedAtAfter(userId, weekAgo)
        val incidentsLastWeek = reportRepository.countByUserIdAndCreatedAtBetween(userId, twoWeeksAgo, weekAgo)
        val incidentsChange = calculateChangePercent(incidentsThisWeek, incidentsLastWeek)

        val regionIds = reportRepository.findDistinctRegionIdsByUserId(userId)
        val dangerousZones = if (regionIds.isEmpty()) 0L else dangerousZoneRepository.countActiveByRegionIdIn(regionIds)
        val zonesThisWeek = if (regionIds.isEmpty()) 0L else dangerousZoneRepository.countByCreatedAtAfterAndRegionIdIn(weekAgo, regionIds)
        val zonesLastWeek = if (regionIds.isEmpty()) 0L else dangerousZoneRepository.countByCreatedAtBetweenAndRegionIdIn(twoWeeksAgo, weekAgo, regionIds)
        val zonesChange = calculateChangePercent(zonesThisWeek, zonesLastWeek)

        val avgRiskScore = if (regionIds.isEmpty()) {
            BigDecimal.ZERO
        } else {
            riskPredictionRepository.findAverageRiskScoreSinceForRegions(weekAgo, regionIds)
                ?: BigDecimal.ZERO
        }
        val safetyGrowth = avgRiskScore.toDouble()
        val avgRiskScoreLastWeek = if (regionIds.isEmpty()) {
            BigDecimal.ZERO
        } else {
            riskPredictionRepository.findAverageRiskScoreSinceForRegions(twoWeeksAgo, regionIds)
                ?: BigDecimal.ZERO
        }
        val safetyChange = calculateChangePercent(avgRiskScore.toDouble(), avgRiskScoreLastWeek.toDouble())

        return DashboardMetricsResponse(
            totalIncidents = totalIncidents,
            incidentsChange = formatChange(incidentsChange),
            activeUsers = 1L,
            usersChange = formatChange(0.0),
            safetyGrowth = safetyGrowth,
            safetyChange = formatChange(safetyChange),
            dangerousZones = dangerousZones,
            zonesChange = formatChange(zonesChange),
        )
    }

    private fun calculateChangePercent(current: Long, previous: Long): Double {
        if (previous == 0L) return if (current > 0) 100.0 else 0.0
        return ((current - previous).toDouble() / previous) * 100.0
    }

    private fun calculateChangePercent(current: Double, previous: Double): Double {
        if (previous == 0.0) return if (current > 0) 100.0 else 0.0
        return ((current - previous) / previous) * 100.0
    }

    private fun formatChange(change: Double): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign${change.toBigDecimal().setScale(1, RoundingMode.HALF_UP)}"
    }
}
