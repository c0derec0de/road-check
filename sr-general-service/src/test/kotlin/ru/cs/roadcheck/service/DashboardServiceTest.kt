package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.repository.DangerousZoneRepository
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.repository.RiskPredictionRepository
import ru.cs.roadcheck.repository.UserRepository
import java.math.BigDecimal

class DashboardServiceTest {

    private val reportRepository: ReportRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val dangerousZoneRepository: DangerousZoneRepository = mockk()
    private val riskPredictionRepository: RiskPredictionRepository = mockk()

    private lateinit var dashboardService: DashboardService

    @BeforeEach
    fun setUp() {
        dashboardService = DashboardService(
            reportRepository = reportRepository,
            userRepository = userRepository,
            dangerousZoneRepository = dangerousZoneRepository,
            riskPredictionRepository = riskPredictionRepository,
        )
    }

    @Test
    fun `getMetrics returns dashboard metrics`() {
        every { reportRepository.count() } returns 100L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 20L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 15L
        every { userRepository.count() } returns 50L
        every { userRepository.countByCreatedAtAfter(any()) } returns 10L
        every { userRepository.countByCreatedAtBetween(any(), any()) } returns 8L
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("65.5")
        every { dangerousZoneRepository.countActive() } returns 25L
        every { dangerousZoneRepository.countActiveCreatedAtAfter(any()) } returns 5L
        every { dangerousZoneRepository.countActiveCreatedAtBetween(any(), any()) } returns 4L

        val result = dashboardService.getMetrics()

        assertThat(result.totalIncidents).isEqualTo(100L)
        assertThat(result.activeUsers).isEqualTo(50L)
        assertThat(result.dangerousZones).isEqualTo(25L)
        assertThat(result.safetyGrowth).isEqualTo(65.5)
        assertThat(result.incidentsChange).isNotNull()
        assertThat(result.usersChange).isNotNull()
        assertThat(result.zonesChange).isNotNull()
        assertThat(result.safetyChange).isNotNull()
    }

    @Test
    fun `getMetrics handles zero previous values`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 5L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 0L
        every { userRepository.count() } returns 5L
        every { userRepository.countByCreatedAtAfter(any()) } returns 2L
        every { userRepository.countByCreatedAtBetween(any(), any()) } returns 0L
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("50")
        every { dangerousZoneRepository.countActive() } returns 3L
        every { dangerousZoneRepository.countActiveCreatedAtAfter(any()) } returns 1L
        every { dangerousZoneRepository.countActiveCreatedAtBetween(any(), any()) } returns 0L

        val result = dashboardService.getMetrics()

        assertThat(result.incidentsChange).isEqualTo("+100.0")
        assertThat(result.usersChange).isEqualTo("+100.0")
        assertThat(result.zonesChange).isEqualTo("+100.0")
    }

    @Test
    fun `getMetrics handles null risk score`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 5L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 5L
        every { userRepository.count() } returns 5L
        every { userRepository.countByCreatedAtAfter(any()) } returns 2L
        every { userRepository.countByCreatedAtBetween(any(), any()) } returns 2L
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns null
        every { dangerousZoneRepository.countActive() } returns 3L
        every { dangerousZoneRepository.countActiveCreatedAtAfter(any()) } returns 1L
        every { dangerousZoneRepository.countActiveCreatedAtBetween(any(), any()) } returns 1L

        val result = dashboardService.getMetrics()

        assertThat(result.safetyGrowth).isEqualTo(0.0)
    }

    @Test
    fun `getMetricsForUser returns user-specific metrics`() {
        val userId = 1L
        every { reportRepository.countByUserId(userId) } returns 10L
        every { reportRepository.countByUserIdAndCreatedAtAfter(userId, any()) } returns 3L
        every { reportRepository.countByUserIdAndCreatedAtBetween(userId, any(), any()) } returns 2L
        every { reportRepository.findDistinctRegionIdsByUserId(userId) } returns listOf(1L, 2L)
        every { dangerousZoneRepository.countActiveByRegionIdIn(listOf(1L, 2L)) } returns 5L
        every { dangerousZoneRepository.countByCreatedAtAfterAndRegionIdIn(any(), listOf(1L, 2L)) } returns 2L
        every { dangerousZoneRepository.countByCreatedAtBetweenAndRegionIdIn(any(), any(), listOf(1L, 2L)) } returns 1L
        every { riskPredictionRepository.findAverageRiskScoreSinceForRegions(any(), listOf(1L, 2L)) } returns BigDecimal("70.0")

        val result = dashboardService.getMetricsForUser(userId)

        assertThat(result.totalIncidents).isEqualTo(10L)
        assertThat(result.activeUsers).isEqualTo(1L)
        assertThat(result.dangerousZones).isEqualTo(5L)
        assertThat(result.safetyGrowth).isEqualTo(70.0)
    }

    @Test
    fun `getMetricsForUser handles empty regions`() {
        val userId = 1L
        every { reportRepository.countByUserId(userId) } returns 5L
        every { reportRepository.countByUserIdAndCreatedAtAfter(userId, any()) } returns 2L
        every { reportRepository.countByUserIdAndCreatedAtBetween(userId, any(), any()) } returns 1L
        every { reportRepository.findDistinctRegionIdsByUserId(userId) } returns emptyList()

        val result = dashboardService.getMetricsForUser(userId)

        assertThat(result.dangerousZones).isEqualTo(0L)
        assertThat(result.safetyGrowth).isEqualTo(0.0)
        assertThat(result.activeUsers).isEqualTo(1L)
    }

    @Test
    fun `getMetricsForUser handles null risk score for user`() {
        val userId = 1L
        every { reportRepository.countByUserId(userId) } returns 5L
        every { reportRepository.countByUserIdAndCreatedAtAfter(userId, any()) } returns 2L
        every { reportRepository.countByUserIdAndCreatedAtBetween(userId, any(), any()) } returns 1L
        every { reportRepository.findDistinctRegionIdsByUserId(userId) } returns listOf(1L)
        every { dangerousZoneRepository.countActiveByRegionIdIn(listOf(1L)) } returns 3L
        every { dangerousZoneRepository.countByCreatedAtAfterAndRegionIdIn(any(), listOf(1L)) } returns 1L
        every { dangerousZoneRepository.countByCreatedAtBetweenAndRegionIdIn(any(), any(), listOf(1L)) } returns 1L
        every { riskPredictionRepository.findAverageRiskScoreSinceForRegions(any(), listOf(1L)) } returns null

        val result = dashboardService.getMetricsForUser(userId)

        assertThat(result.safetyGrowth).isEqualTo(0.0)
    }

    @Test
    fun `formatChange adds plus sign for positive values`() {
        // This is tested indirectly through getMetrics
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 6L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 4L
        every { userRepository.count() } returns 5L
        every { userRepository.countByCreatedAtAfter(any()) } returns 3L
        every { userRepository.countByCreatedAtBetween(any(), any()) } returns 2L
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("50")
        every { dangerousZoneRepository.countActive() } returns 3L
        every { dangerousZoneRepository.countActiveCreatedAtAfter(any()) } returns 2L
        every { dangerousZoneRepository.countActiveCreatedAtBetween(any(), any()) } returns 1L

        val result = dashboardService.getMetrics()

        assertThat(result.incidentsChange).startsWith("+")
    }

    @Test
    fun `formatChange handles negative values`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 3L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 6L
        every { userRepository.count() } returns 5L
        every { userRepository.countByCreatedAtAfter(any()) } returns 1L
        every { userRepository.countByCreatedAtBetween(any(), any()) } returns 3L
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("30")
        every { dangerousZoneRepository.countActive() } returns 3L
        every { dangerousZoneRepository.countActiveCreatedAtAfter(any()) } returns 1L
        every { dangerousZoneRepository.countActiveCreatedAtBetween(any(), any()) } returns 2L

        val result = dashboardService.getMetrics()

        assertThat(result.incidentsChange).startsWith("-")
    }

    @Test
    fun `calculateChangePercent handles equal values`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 5L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 5L
        every { userRepository.count() } returns 5L
        every { userRepository.countByCreatedAtAfter(any()) } returns 2L
        every { userRepository.countByCreatedAtBetween(any(), any()) } returns 2L
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("50")
        every { dangerousZoneRepository.countActive() } returns 3L
        every { dangerousZoneRepository.countActiveCreatedAtAfter(any()) } returns 1L
        every { dangerousZoneRepository.countActiveCreatedAtBetween(any(), any()) } returns 1L

        val result = dashboardService.getMetrics()

        assertThat(result.incidentsChange).isEqualTo("+0.0")
    }
}
