package ru.cs.roadcheck.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jobs")
data class JobsProperties(
    val predictions: Predictions = Predictions(),
    val reports: Reports = Reports(),
) {
    data class Predictions(
        val script: Script = Script(),
        val intervalMinutes: Int = 10,
    )

    data class Reports(
        val archiveCron: String = "0 0 0 * * ?",
    )

    data class Script(
        val path: String = "src/main/resources/scripts/risk.py",
    )
}
