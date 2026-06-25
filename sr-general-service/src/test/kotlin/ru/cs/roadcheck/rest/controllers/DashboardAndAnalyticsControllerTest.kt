package ru.cs.roadcheck.rest.controllers

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import ru.cs.roadcheck.rest.dto.CurrentWeatherResponse
import ru.cs.roadcheck.rest.dto.DashboardMetricsResponse
import ru.cs.roadcheck.rest.dto.DangerousZonesListResponse
import ru.cs.roadcheck.rest.dto.ReportMetricsResponse
import ru.cs.roadcheck.rest.dto.WeatherInfo
import ru.cs.roadcheck.service.AnalyticsService
import ru.cs.roadcheck.service.DashboardService
import ru.cs.roadcheck.service.ReportMetricsService
import ru.cs.roadcheck.service.WeatherService

class DashboardAndAnalyticsControllerTest {

    private val dashboardService: DashboardService = mockk()
    private val reportMetricsService: ReportMetricsService = mockk()
    private val weatherService: WeatherService = mockk()
    private val analyticsService: AnalyticsService = mockk()

    private lateinit var dashboardController: DashboardController
    private lateinit var analyticsController: AnalyticsController

    private fun user(id: Long) = UsernamePasswordAuthenticationToken(
        id,
        null,
        listOf(SimpleGrantedAuthority("ROLE_USER")),
    )

    private fun mod(id: Long) = UsernamePasswordAuthenticationToken(
        id,
        null,
        listOf(SimpleGrantedAuthority("ROLE_MODERATOR")),
    )

    @BeforeEach
    fun setUp() {
        dashboardController = DashboardController(dashboardService, reportMetricsService, weatherService)
        analyticsController = AnalyticsController(analyticsService)
    }

    @Test
    fun `dashboard metrics moderator`() {
        every { dashboardService.getMetrics() } returns DashboardMetricsResponse(
            totalIncidents = 1L,
            incidentsChange = "0%",
            activeUsers = 2L,
            usersChange = "0%",
            safetyGrowth = 0.0,
            safetyChange = "0%",
            dangerousZones = 3L,
            zonesChange = "0%",
        )

        val result = dashboardController.getMetrics(mod(1L))

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
        assertThat(result.body?.totalIncidents).isEqualTo(1L)
    }

    @Test
    fun `dashboard metrics user`() {
        every { dashboardService.getMetricsForUser(5L) } returns DashboardMetricsResponse(
            totalIncidents = 0L,
            incidentsChange = "0%",
            activeUsers = 0L,
            usersChange = "0%",
            safetyGrowth = 0.0,
            safetyChange = "0%",
            dangerousZones = 0L,
            zonesChange = "0%",
        )

        val result = dashboardController.getMetrics(user(5L))

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
    }

    @Test
    fun `report metrics`() {
        every { reportMetricsService.getReportMetrics() } returns ReportMetricsResponse(
            total = 1L,
            totalChange = "0%",
            new = 0L,
            newChange = "0%",
            inProgress = 0L,
            inProgressChange = "0%",
            completed = 0L,
            completedChange = "0%",
        )

        val result = dashboardController.getReportMetrics(mod(1L))

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
    }

    @Test
    fun `current weather`() {
        every { weatherService.getCurrentWeather(null, null) } returns CurrentWeatherResponse(
            weather = WeatherInfo(
                condition = "clear",
                temperature = 0,
                unit = "C",
                windSpeedKmh = null,
                humidity = null,
            ),
            riskLevel = "low",
            lastUpdated = null,
        )

        val result = dashboardController.getCurrentWeather(null, null)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
    }

    @Test
    fun `analytics dangerous zones moderator`() {
        every { analyticsService.getDangerousZones(null) } returns DangerousZonesListResponse(zones = emptyList(), total = 0)

        val result = analyticsController.getDangerousZones(mod(1L), null)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
    }

    @Test
    fun `analytics dangerous zones user`() {
        every { analyticsService.getDangerousZonesForUser(2L, null) } returns DangerousZonesListResponse(zones = emptyList(), total = 0)

        val result = analyticsController.getDangerousZones(user(2L), null)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
    }
}
