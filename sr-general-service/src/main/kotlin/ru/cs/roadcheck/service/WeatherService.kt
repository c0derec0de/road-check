package ru.cs.roadcheck.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.cs.roadcheck.repository.RiskPredictionRepository
import ru.cs.roadcheck.repository.WeatherRepository
import ru.cs.roadcheck.rest.dto.CurrentWeatherResponse
import ru.cs.roadcheck.rest.dto.WeatherInfo
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@Service
class WeatherService(
    private val weatherRepository: WeatherRepository,
    private val riskPredictionRepository: RiskPredictionRepository,
    private val externalWeatherClient: ExternalWeatherClient,
) {

    private val defaultLat = BigDecimal("55.755826")
    private val defaultLng = BigDecimal("37.617299")

    fun getCurrentWeather(lat: BigDecimal?, lng: BigDecimal?): CurrentWeatherResponse {
        val latitude = lat ?: defaultLat
        val longitude = lng ?: defaultLng
        logger.debug { "Getting weather for coordinates: lat=$latitude, lng=$longitude" }

        val weatherFromDb = weatherRepository.findLatestByCoordinates(latitude, longitude)
        logger.debug { "Found ${weatherFromDb.size} weather records in DB" }

        val weatherInfoFromDb = weatherFromDb.firstOrNull()?.let { latest ->
            WeatherInfo(
                condition = latest.currentWeather,
                temperature = latest.temperature?.toInt(),
                unit = "C",
                windSpeedKmh = latest.windSpeed,
                humidity = latest.humidity?.toInt(),
            )
        }
        val external = externalWeatherClient.getNow(latitude, longitude)
        if (external != null) {
            logger.debug {
                "External weather: temp=${external.temperature}, precip=${external.precipitation}, " +
                    "wind=${external.windSpeedKmh}, humidity=${external.humidity}"
            }
        }

        val weather = weatherInfoFromDb ?: WeatherInfo(
            condition = null,
            temperature = external?.temperature?.toInt(),
            unit = "C",
            windSpeedKmh = external?.windSpeedKmh,
            humidity = external?.humidity,
        )
        val weekAgo = Instant.now().minusSeconds(7 * 24 * 60 * 60)
        val avgRisk = riskPredictionRepository.findAverageRiskScoreSince(weekAgo)
        logger.debug { "Average risk score: $avgRisk" }
        val riskLevel = when {
            avgRisk == null -> null
            avgRisk.toDouble() >= 70 -> "Высокий"
            avgRisk.toDouble() >= 40 -> "Средний"
            else -> "Низкий"
        }

        val lastUpdated = LocalTime.now(ZoneId.of("Europe/Moscow"))
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        logger.info { "Weather retrieved: condition=${weather.condition}, temperature=${weather.temperature}, riskLevel=$riskLevel" }
        return CurrentWeatherResponse(
            weather = weather,
            riskLevel = riskLevel,
            lastUpdated = lastUpdated,
        )
    }
}
