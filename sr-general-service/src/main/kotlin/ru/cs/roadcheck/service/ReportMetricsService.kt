package ru.cs.roadcheck.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.rest.dto.ReportMetricsResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.math.RoundingMode

private val logger = KotlinLogging.logger {}

@Service
class ReportMetricsService(
    private val reportRepository: ReportRepository,
) {

    fun getReportMetrics(): ReportMetricsResponse {
        logger.debug { "Calculating report metrics" }
        val now = Instant.now()
        val weekAgo = now.minus(7, ChronoUnit.DAYS)
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)

        val total = reportRepository.count()
        logger.debug { "Total reports: $total" }
        val totalThisWeek = reportRepository.countByCreatedAtAfter(weekAgo)
        val totalLastWeek = reportRepository.countByCreatedAtBetween(twoWeeksAgo, weekAgo)
        val totalChange = formatChange(calculateChangePercent(totalThisWeek, totalLastWeek))

        val new = reportRepository.countByStatus(ReportStatus.NEW.name)
        logger.debug { "New reports: $new" }
        val newThisWeek = reportRepository.countByStatusAndCreatedAtAfter(ReportStatus.NEW.name, weekAgo)
        val newLastWeek = reportRepository.countByStatusAndCreatedAtBetween(ReportStatus.NEW.name, twoWeeksAgo, weekAgo)
        val newChange = formatChange(calculateChangePercent(newThisWeek, newLastWeek))

        val inProgress = reportRepository.countByStatus(ReportStatus.IN_PROGRESS.name)
        logger.debug { "In progress reports: $inProgress" }
        val inProgressThisWeek = reportRepository.countByStatusAndCreatedAtAfter(ReportStatus.IN_PROGRESS.name, weekAgo)
        val inProgressLastWeek = reportRepository.countByStatusAndCreatedAtBetween(ReportStatus.IN_PROGRESS.name, twoWeeksAgo, weekAgo)
        val inProgressChange = formatChange(calculateChangePercent(inProgressThisWeek, inProgressLastWeek))

        val completed = reportRepository.countByStatus(ReportStatus.CONFIRMED.name)
        logger.debug { "Completed reports: $completed" }
        val completedThisWeek = reportRepository.countByStatusAndCreatedAtAfter(ReportStatus.CONFIRMED.name, weekAgo)
        val completedLastWeek = reportRepository.countByStatusAndCreatedAtBetween(ReportStatus.CONFIRMED.name, twoWeeksAgo, weekAgo)
        val completedChange = formatChange(calculateChangePercent(completedThisWeek, completedLastWeek))

        logger.info { "Report metrics calculated: total=$total, new=$new, inProgress=$inProgress, completed=$completed" }
        return ReportMetricsResponse(
            total = total,
            totalChange = totalChange,
            new = new,
            newChange = newChange,
            inProgress = inProgress,
            inProgressChange = inProgressChange,
            completed = completed,
            completedChange = completedChange,
        )
    }

    fun getReportMetricsForUser(userId: Long): ReportMetricsResponse {
        logger.debug { "Calculating report metrics for userId=$userId" }
        val now = Instant.now()
        val weekAgo = now.minus(7, ChronoUnit.DAYS)
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)

        val total = reportRepository.countByUserId(userId)
        val totalThisWeek = reportRepository.countByUserIdAndCreatedAtAfter(userId, weekAgo)
        val totalLastWeek = reportRepository.countByUserIdAndCreatedAtBetween(userId, twoWeeksAgo, weekAgo)
        val totalChange = formatChange(calculateChangePercent(totalThisWeek, totalLastWeek))

        val new = reportRepository.countByUserIdAndStatus(userId, ReportStatus.NEW)
        val newThisWeek = reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.NEW, weekAgo)
        val newLastWeek = reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.NEW, twoWeeksAgo, weekAgo)
        val newChange = formatChange(calculateChangePercent(newThisWeek, newLastWeek))

        val inProgress = reportRepository.countByUserIdAndStatus(userId, ReportStatus.IN_PROGRESS)
        val inProgressThisWeek = reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.IN_PROGRESS, weekAgo)
        val inProgressLastWeek = reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.IN_PROGRESS, twoWeeksAgo, weekAgo)
        val inProgressChange = formatChange(calculateChangePercent(inProgressThisWeek, inProgressLastWeek))

        val completed = reportRepository.countByUserIdAndStatus(userId, ReportStatus.CONFIRMED)
        val completedThisWeek = reportRepository.countByUserIdAndStatusAndCreatedAtAfter(userId, ReportStatus.CONFIRMED, weekAgo)
        val completedLastWeek = reportRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReportStatus.CONFIRMED, twoWeeksAgo, weekAgo)
        val completedChange = formatChange(calculateChangePercent(completedThisWeek, completedLastWeek))

        return ReportMetricsResponse(
            total = total,
            totalChange = totalChange,
            new = new,
            newChange = newChange,
            inProgress = inProgress,
            inProgressChange = inProgressChange,
            completed = completed,
            completedChange = completedChange,
        )
    }

    private fun calculateChangePercent(current: Long, previous: Long): Double {
        if (previous == 0L) return if (current > 0) 100.0 else 0.0
        return ((current - previous).toDouble() / previous) * 100.0
    }

    private fun formatChange(change: Double): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign${change.toBigDecimal().setScale(1, RoundingMode.HALF_UP)}"
    }
}
