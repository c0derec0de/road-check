package ru.cs.roadcheck.config

import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.cs.roadcheck.job.ArchiveReportsJob
import ru.cs.roadcheck.job.PredictionJob

@Configuration
@EnableConfigurationProperties(JobsProperties::class)
class JobConfig(
    private val jobsProperties: JobsProperties,
) {

    @Bean
    fun predictionJobDetail(): JobDetail {
        return JobBuilder.newJob(PredictionJob::class.java)
            .withIdentity("predictionJob", "predictionGroup")
            .storeDurably()
            .build()
    }

    @Bean
    fun predictionJobTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(predictionJobDetail())
            .withIdentity("predictionTrigger", "predictionGroup")
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMinutes(jobsProperties.predictions.intervalMinutes)
                    .repeatForever()
            )
            .build()
    }

    @Bean
    fun archiveReportsJobDetail(): JobDetail {
        return JobBuilder.newJob(ArchiveReportsJob::class.java)
            .withIdentity("archiveReportsJob", "reportsGroup")
            .storeDurably()
            .build()
    }

    @Bean
    fun archiveReportsJobTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(archiveReportsJobDetail())
            .withIdentity("archiveReportsTrigger", "reportsGroup")
            .withSchedule(CronScheduleBuilder.cronSchedule(jobsProperties.reports.archiveCron))
            .build()
    }
}
