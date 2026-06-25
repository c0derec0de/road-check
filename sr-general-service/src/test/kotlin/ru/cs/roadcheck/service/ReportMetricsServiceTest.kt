package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.repository.ReportRepository

class ReportMetricsServiceTest {

    private val reportRepository: ReportRepository = mockk()

    private lateinit var reportMetricsService: ReportMetricsService

    @BeforeEach
    fun setUp() {
        reportMetricsService = ReportMetricsService(
            reportRepository = reportRepository,
        )
    }

    @Test
    fun `getReportMetrics returns report metrics`() {
        every { reportRepository.count() } returns 100L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 20L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 15L
        every { reportRepository.countByStatus("NEW") } returns 30L
        every { reportRepository.countByStatusAndCreatedAtAfter("NEW", any()) } returns 8L
        every { reportRepository.countByStatusAndCreatedAtBetween("NEW", any(), any()) } returns 5L
        every { reportRepository.countByStatus("IN_PROGRESS") } returns 25L
        every { reportRepository.countByStatusAndCreatedAtAfter("IN_PROGRESS", any()) } returns 6L
        every { reportRepository.countByStatusAndCreatedAtBetween("IN_PROGRESS", any(), any()) } returns 4L
        every { reportRepository.countByStatus("CONFIRMED") } returns 45L
        every { reportRepository.countByStatusAndCreatedAtAfter("CONFIRMED", any()) } returns 10L
        every { reportRepository.countByStatusAndCreatedAtBetween("CONFIRMED", any(), any()) } returns 7L

        val result = reportMetricsService.getReportMetrics()

        assertThat(result.total).isEqualTo(100L)
        assertThat(result.new).isEqualTo(30L)
        assertThat(result.inProgress).isEqualTo(25L)
        assertThat(result.completed).isEqualTo(45L)
        assertThat(result.totalChange).isNotNull()
        assertThat(result.newChange).isNotNull()
        assertThat(result.inProgressChange).isNotNull()
        assertThat(result.completedChange).isNotNull()
    }

    @Test
    fun `getReportMetrics handles zero previous values`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 5L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 0L
        every { reportRepository.countByStatus("NEW") } returns 5L
        every { reportRepository.countByStatusAndCreatedAtAfter("NEW", any()) } returns 3L
        every { reportRepository.countByStatusAndCreatedAtBetween("NEW", any(), any()) } returns 0L
        every { reportRepository.countByStatus("IN_PROGRESS") } returns 3L
        every { reportRepository.countByStatusAndCreatedAtAfter("IN_PROGRESS", any()) } returns 2L
        every { reportRepository.countByStatusAndCreatedAtBetween("IN_PROGRESS", any(), any()) } returns 0L
        every { reportRepository.countByStatus("CONFIRMED") } returns 2L
        every { reportRepository.countByStatusAndCreatedAtAfter("CONFIRMED", any()) } returns 1L
        every { reportRepository.countByStatusAndCreatedAtBetween("CONFIRMED", any(), any()) } returns 0L

        val result = reportMetricsService.getReportMetrics()

        assertThat(result.totalChange).isEqualTo("+100.0")
        assertThat(result.newChange).isEqualTo("+100.0")
        assertThat(result.inProgressChange).isEqualTo("+100.0")
        assertThat(result.completedChange).isEqualTo("+100.0")
    }

    @Test
    fun `getReportMetrics handles equal values`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 5L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 5L
        every { reportRepository.countByStatus("NEW") } returns 5L
        every { reportRepository.countByStatusAndCreatedAtAfter("NEW", any()) } returns 2L
        every { reportRepository.countByStatusAndCreatedAtBetween("NEW", any(), any()) } returns 2L
        every { reportRepository.countByStatus("IN_PROGRESS") } returns 3L
        every { reportRepository.countByStatusAndCreatedAtAfter("IN_PROGRESS", any()) } returns 1L
        every { reportRepository.countByStatusAndCreatedAtBetween("IN_PROGRESS", any(), any()) } returns 1L
        every { reportRepository.countByStatus("CONFIRMED") } returns 2L
        every { reportRepository.countByStatusAndCreatedAtAfter("CONFIRMED", any()) } returns 1L
        every { reportRepository.countByStatusAndCreatedAtBetween("CONFIRMED", any(), any()) } returns 1L

        val result = reportMetricsService.getReportMetrics()

        assertThat(result.totalChange).isEqualTo("+0.0")
        assertThat(result.newChange).isEqualTo("+0.0")
        assertThat(result.inProgressChange).isEqualTo("+0.0")
        assertThat(result.completedChange).isEqualTo("+0.0")
    }

    @Test
    fun `getReportMetrics handles negative change`() {
        every { reportRepository.count() } returns 10L
        every { reportRepository.countByCreatedAtAfter(any()) } returns 3L
        every { reportRepository.countByCreatedAtBetween(any(), any()) } returns 6L
        every { reportRepository.countByStatus("NEW") } returns 5L
        every { reportRepository.countByStatusAndCreatedAtAfter("NEW", any()) } returns 1L
        every { reportRepository.countByStatusAndCreatedAtBetween("NEW", any(), any()) } returns 3L
        every { reportRepository.countByStatus("IN_PROGRESS") } returns 3L
        every { reportRepository.countByStatusAndCreatedAtAfter("IN_PROGRESS", any()) } returns 1L
        every { reportRepository.countByStatusAndCreatedAtBetween("IN_PROGRESS", any(), any()) } returns 2L
        every { reportRepository.countByStatus("CONFIRMED") } returns 2L
        every { reportRepository.countByStatusAndCreatedAtAfter("CONFIRMED", any()) } returns 0L
        every { reportRepository.countByStatusAndCreatedAtBetween("CONFIRMED", any(), any()) } returns 2L

        val result = reportMetricsService.getReportMetrics()

        assertThat(result.totalChange).startsWith("-")
        assertThat(result.newChange).startsWith("-")
        assertThat(result.inProgressChange).startsWith("-")
        assertThat(result.completedChange).startsWith("-")
    }

    @Test
    fun `getReportMetricsForUser returns user-specific metrics`() {
        val userId = 1L
        every { reportRepository.countByUserId(userId) } returns 20L
        every { reportRepository.countByUserIdAndCreatedAtAfter(userId, any()) } returns 5L
        every { reportRepository.countByUserIdAndCreatedAtBetween(userId, any(), any()) } returns 3L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.NEW) } returns 8L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.NEW, any()) } returns 2L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.NEW, any(), any()) } returns 1L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.IN_PROGRESS) } returns 6L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.IN_PROGRESS, any()) } returns 2L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.IN_PROGRESS, any(), any()) } returns 1L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.CONFIRMED) } returns 6L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.CONFIRMED, any()) } returns 2L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.CONFIRMED, any(), any()) } returns 1L

        val result = reportMetricsService.getReportMetricsForUser(userId)

        assertThat(result.total).isEqualTo(20L)
        assertThat(result.new).isEqualTo(8L)
        assertThat(result.inProgress).isEqualTo(6L)
        assertThat(result.completed).isEqualTo(6L)
        assertThat(result.totalChange).isNotNull()
        assertThat(result.newChange).isNotNull()
        assertThat(result.inProgressChange).isNotNull()
        assertThat(result.completedChange).isNotNull()
    }

    @Test
    fun `getReportMetricsForUser handles empty user data`() {
        val userId = 1L
        every { reportRepository.countByUserId(userId) } returns 0L
        every { reportRepository.countByUserIdAndCreatedAtAfter(userId, any()) } returns 0L
        every { reportRepository.countByUserIdAndCreatedAtBetween(userId, any(), any()) } returns 0L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.NEW) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.NEW, any()) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.NEW, any(), any()) } returns 0L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.IN_PROGRESS) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.IN_PROGRESS, any()) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.IN_PROGRESS, any(), any()) } returns 0L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.CONFIRMED) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.CONFIRMED, any()) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.CONFIRMED, any(), any()) } returns 0L

        val result = reportMetricsService.getReportMetricsForUser(userId)

        assertThat(result.total).isEqualTo(0L)
        assertThat(result.new).isEqualTo(0L)
        assertThat(result.inProgress).isEqualTo(0L)
        assertThat(result.completed).isEqualTo(0L)
        assertThat(result.totalChange).isEqualTo("+0.0")
    }

    @Test
    fun `getReportMetricsForUser with single user report`() {
        val userId = 1L
        every { reportRepository.countByUserId(userId) } returns 1L
        every { reportRepository.countByUserIdAndCreatedAtAfter(userId, any()) } returns 1L
        every { reportRepository.countByUserIdAndCreatedAtBetween(userId, any(), any()) } returns 0L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.NEW) } returns 1L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.NEW, any()) } returns 1L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.NEW, any(), any()) } returns 0L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.IN_PROGRESS) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.IN_PROGRESS, any()) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.IN_PROGRESS, any(), any()) } returns 0L
        every { reportRepository.countByUserIdAndStatus(userId, ReportStatus.CONFIRMED) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.CONFIRMED, any()) } returns 0L
        every { reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.CONFIRMED, any(), any()) } returns 0L

        val result = reportMetricsService.getReportMetricsForUser(userId)

        assertThat(result.total).isEqualTo(1L)
        assertThat(result.totalChange).isEqualTo("+100.0")
        assertThat(result.newChange).isEqualTo("+100.0")
    }
}
