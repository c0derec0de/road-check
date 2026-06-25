package ru.cs.roadcheck.rest.controllers

import org.quartz.JobKey
import org.quartz.Scheduler
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity
import ru.cs.roadcheck.service.SvgChartService

@RestController
@RequestMapping("/api/predictions")
class PredictionController(
    private val scheduler: Scheduler,
    private val svgChartService: SvgChartService
) {

    @PostMapping("/run-manual")
    fun runPredictionJobManually(): ResponseEntity<String> {
        return try {
            scheduler.triggerJob(JobKey("predictionJob", "predictionGroup"))
            ResponseEntity.ok("Prediction job triggered successfully")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("Error triggering prediction job: ${e.message}")
        }
    }

    // Эндпоинт для получения графика ДТП по месяцам
    @GetMapping("/charts/monthly", produces = ["image/svg+xml"])
    fun getMonthlyAccidentsChart(): ResponseEntity<String> {
        val svg = svgChartService.generateMonthlyAccidentsChart()
        return createSvgResponse(svg)
    }

    // Эндпоинт для распределения рисков
    @GetMapping("/charts/risk", produces = ["image/svg+xml"])
    fun getRiskDistributionChart(): ResponseEntity<String> {
        val svg = svgChartService.generateRiskDistributionChart()
        return createSvgResponse(svg)
    }

    // Эндпоинт для причин ДТП
    @GetMapping("/charts/causes", produces = ["image/svg+xml"])
    fun getCausesDistributionChart(): ResponseEntity<String> {
        val svg = svgChartService.generateCausesDistributionChart()
        return createSvgResponse(svg)
    }

    private fun createSvgResponse(svg: String): ResponseEntity<String> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .body(svg)
    }
}