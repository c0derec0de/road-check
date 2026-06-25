package ru.cs.roadcheck.rest.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.Authentication
import ru.cs.roadcheck.auth.isModerator
import ru.cs.roadcheck.auth.userId
import ru.cs.roadcheck.rest.dto.CurrentWeatherResponse
import ru.cs.roadcheck.rest.dto.DashboardMetricsResponse
import ru.cs.roadcheck.rest.dto.ReportMetricsResponse
import ru.cs.roadcheck.service.DashboardService
import ru.cs.roadcheck.service.ReportMetricsService
import ru.cs.roadcheck.service.WeatherService
import java.math.BigDecimal

@Tag(name = "Дашборд", description = "Метрики и аналитика для дашборда")
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
    private val reportMetricsService: ReportMetricsService,
    private val weatherService: WeatherService,
) {

    @Operation(
        summary = "Общие метрики дашборда",
        description = "Возвращает общие метрики: количество инцидентов, пользователей, опасных зон, уровень безопасности и их изменения за неделю.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Метрики получены", content = [Content(schema = Schema(implementation = DashboardMetricsResponse::class))]),
        ],
    )
    @GetMapping("/metrics")
    fun getMetrics(auth: Authentication) = ResponseEntity.ok(
        if (auth.isModerator()) dashboardService.getMetrics() else dashboardService.getMetricsForUser(auth.userId()),
    )

    @Operation(
        summary = "Метрики по отчётам",
        description = "Возвращает метрики по отчётам: общее количество, количество по статусам (NEW, IN_PROGRESS, CONFIRMED) и их изменения за неделю.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Метрики получены", content = [Content(schema = Schema(implementation = ReportMetricsResponse::class))]),
        ],
    )
    @GetMapping("/report-metrics")
    fun getReportMetrics(auth: Authentication) = ResponseEntity.ok(
        if (auth.isModerator()) reportMetricsService.getReportMetrics()
        else reportMetricsService.getReportMetricsForUser(auth.userId()),
    )

    @Operation(
        summary = "Текущая погода и уровень риска",
        description = "Возвращает текущую погоду по координатам (по умолчанию Москва) и уровень риска на основе последних предсказаний.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Данные получены", content = [Content(schema = Schema(implementation = CurrentWeatherResponse::class))]),
        ],
    )
    @GetMapping("/current")
    fun getCurrentWeather(
        @Parameter(description = "Широта (по умолчанию Москва)", example = "55.755826")
        @RequestParam(required = false) lat: BigDecimal?,
        @Parameter(description = "Долгота (по умолчанию Москва)", example = "37.617299")
        @RequestParam(required = false) lng: BigDecimal?,
    ) = ResponseEntity.ok(weatherService.getCurrentWeather(lat, lng))
}
