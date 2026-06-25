package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.common.domain.entities.Weather
import ru.cs.roadcheck.repository.RiskPredictionRepository
import ru.cs.roadcheck.repository.WeatherRepository
import java.math.BigDecimal
import java.time.Instant

class WeatherServiceTest {

    private val weatherRepository: WeatherRepository = mockk()
    private val riskPredictionRepository: RiskPredictionRepository = mockk()
    private val externalWeatherClient: ExternalWeatherClient = mockk()

    private val service = WeatherService(
        weatherRepository = weatherRepository,
        riskPredictionRepository = riskPredictionRepository,
        externalWeatherClient = externalWeatherClient,
    )

    @Test
    fun `getCurrentWeather uses DB weather when available and computes high risk level`() {
        val weatherEntity = Weather().apply {
            temperature = BigDecimal("5.5")
            currentWeather = "Облачно"
            humidity = BigDecimal("80")
            windSpeed = BigDecimal("10.0")
            latitude = BigDecimal("55.755826")
            longitude = BigDecimal("37.617299")
            timestamp = Instant.now()
        }
        every { weatherRepository.findLatestByCoordinates(any(), any()) } returns listOf(weatherEntity)
        every { externalWeatherClient.getNow(any(), any()) } returns null
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("75")

        val result = service.getCurrentWeather(null, null)

        assertThat(result.weather.condition).isEqualTo("Облачно")
        assertThat(result.weather.temperature).isEqualTo(5)
        assertThat(result.weather.windSpeedKmh).isEqualByComparingTo("10.0")
        assertThat(result.weather.humidity).isEqualTo(80)
        assertThat(result.riskLevel).isEqualTo("Высокий")
        assertThat(result.lastUpdated).isNotNull()
    }

    @Test
    fun `getCurrentWeather falls back to external weather and computes medium risk level`() {
        every { weatherRepository.findLatestByCoordinates(any(), any()) } returns emptyList()
        every { externalWeatherClient.getNow(any(), any()) } returns
            ExternalWeatherClient.ExternalWeatherResult(
                temperature = BigDecimal("7.0"),
                precipitation = BigDecimal("0.3"),
                windSpeedKmh = BigDecimal("8.9"),
                humidity = 75,
            )
        every { riskPredictionRepository.findAverageRiskScoreSince(any()) } returns BigDecimal("50")

        val result = service.getCurrentWeather(BigDecimal("52.52"), BigDecimal("13.419998"))

        assertThat(result.weather.condition).isNull()
        assertThat(result.weather.temperature).isEqualTo(7)
        assertThat(result.weather.windSpeedKmh).isEqualByComparingTo("8.9")
        assertThat(result.weather.humidity).isEqualTo(75)
        assertThat(result.riskLevel).isEqualTo("Средний")
    }
}

