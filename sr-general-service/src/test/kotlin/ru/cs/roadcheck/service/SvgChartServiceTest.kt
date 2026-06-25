package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.common.domain.entities.DangerousZone
import ru.cs.roadcheck.common.domain.entities.Report
import ru.cs.roadcheck.repository.DangerousZoneRepository
import ru.cs.roadcheck.repository.ReportRepository
import java.time.Instant

class SvgChartServiceTest {

    private val reportRepository: ReportRepository = mockk()
    private val dangerousZoneRepository: DangerousZoneRepository = mockk()
    private lateinit var service: SvgChartService

    @BeforeEach
    fun setUp() {
        service = SvgChartService(reportRepository, dangerousZoneRepository)
    }

    @Test
    fun `monthly chart returns svg wrapper`() {
        every { reportRepository.findAll() } returns emptyList()

        val svg = service.generateMonthlyAccidentsChart()

        assertThat(svg).contains("<svg")
        assertThat(svg).contains("Динамика происшествий")
    }

    @Test
    fun `risk chart handles empty zones`() {
        every { dangerousZoneRepository.findByIsActiveTrue() } returns emptyList()

        val svg = service.generateRiskDistributionChart()

        assertThat(svg).contains("Данные отсутствуют")
    }

    @Test
    fun `risk chart aggregates levels`() {
        val z1 = DangerousZone().apply {
            id = 1L
            riskLevel = "high"
        }
        val z2 = DangerousZone().apply {
            id = 2L
            riskLevel = "medium"
        }
        every { dangerousZoneRepository.findByIsActiveTrue() } returns listOf(z1, z2)

        val svg = service.generateRiskDistributionChart()

        assertThat(svg).contains("Высокий")
        assertThat(svg).contains("Средний")
    }

    @Test
    fun `causes chart empty data`() {
        every { reportRepository.findAll() } returns emptyList()

        val svg = service.generateCausesDistributionChart()

        assertThat(svg).contains("Данных по причинам нет")
    }

    @Test
    fun `causes chart with causes`() {
        val r = Report().apply {
            id = 1L
            cause = "Снег"
            createdAt = Instant.now()
        }
        every { reportRepository.findAll() } returns listOf(r)

        val svg = service.generateCausesDistributionChart()

        assertThat(svg).contains("Снег")
    }
}
