package ru.cs.roadcheck.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal

private val externalLogger = KotlinLogging.logger {}

@Component
class ExternalWeatherClient(
    private val restTemplate: RestTemplate = RestTemplate(),
) {

    data class ExternalWeatherResult(
        val temperature: BigDecimal?,
        val precipitation: BigDecimal?,
        val windSpeedKmh: BigDecimal?,
        val humidity: Int?,
    )

    fun getNow(lat: BigDecimal, lng: BigDecimal): ExternalWeatherResult? {
        return try {
            val url =
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat" +
                    "&longitude=$lng" +
                    "&current=temperature_2m,wind_speed_10m" +
                    "&hourly=relative_humidity_2m,precipitation"
            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.getForObject(url, Map::class.java) as? Map<*, *>
            val current = response?.get("current") as? Map<*, *>
            val hourly = response?.get("hourly") as? Map<*, *>

            val temp = (current?.get("temperature_2m") as? Number)
                ?.toDouble()
                ?.let { BigDecimal.valueOf(it) }
            val windSpeed = (current?.get("wind_speed_10m") as? Number)
                ?.toDouble()
                ?.let { BigDecimal.valueOf(it) }

            var humidity: Int? = null
            var precipMm: BigDecimal? = null

            val currentTime = current?.get("time") as? String
            if (currentTime != null && hourly != null) {
                val times = hourly["time"] as? List<*>
                val humidityList = hourly["relative_humidity_2m"] as? List<*>
                val precipList = hourly["precipitation"] as? List<*>
                val idx = times?.indexOf(currentTime) ?: -1
                if (idx >= 0) {
                    humidity = (humidityList?.getOrNull(idx) as? Number)?.toInt()
                    precipMm = (precipList?.getOrNull(idx) as? Number)
                        ?.toDouble()
                        ?.let { BigDecimal.valueOf(it) }
                }
            }

            ExternalWeatherResult(
                temperature = temp,
                precipitation = precipMm,
                windSpeedKmh = windSpeed,
                humidity = humidity,
            )
        } catch (ex: Exception) {
            externalLogger.error(ex) { "Error while calling external weather API" }
            null
        }
    }
}
