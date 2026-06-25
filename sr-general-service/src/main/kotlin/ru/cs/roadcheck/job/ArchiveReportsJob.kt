package ru.cs.roadcheck.job

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.cs.roadcheck.service.ReportService

@Component
class ArchiveReportsJob(
    private val reportService: ReportService,
) : Job {

    private val logger = LoggerFactory.getLogger(ArchiveReportsJob::class.java)

    override fun execute(context: JobExecutionContext?) {
        val archived = reportService.archiveProcessedReports()
        logger.info("Archive reports job completed. Archived reports: {}", archived)
    }
}
