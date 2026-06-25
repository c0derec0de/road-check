package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal

class ExternalWeatherClientTest {

    private val restTemplate: RestTemplate = mockk()
    private val client = ExternalWeatherClient(restTemplate)

    @Test
    fun `getNow parses current and hourly fields from Open-Meteo response`() {
        val response: Map<String, Any?> = mapOf(
            "current" to mapOf(
                "time" to "2026-03-05T20:30",
                "temperature_2m" to 7.0,
                "wind_speed_10m" to 8.9,
            ),
            "hourly" to mapOf(
                "time" to listOf("2026-03-05T20:00", "2026-03-05T20:30"),
                "relative_humidity_2m" to listOf(70, 75),
                "precipitation" to listOf(0.0, 0.3),
            ),
        )

        every { restTemplate.getForObject(any<String>(), Map::class.java) } returns response

        val result = client.getNow(BigDecimal("52.52"), BigDecimal("13.419998"))

        assertThat(result).isNotNull
        assertThat(result!!.temperature).isEqualByComparingTo("7.0")
        assertThat(result.windSpeedKmh).isEqualByComparingTo("8.9")
        assertThat(result.humidity).isEqualTo(75)
        assertThat(result.precipitation).isEqualByComparingTo("0.3")
    }

    @Test
    fun `getNow returns null when API call fails`() {
        every { restTemplate.getForObject(any<String>(), Map::class.java) } throws RuntimeException("boom")

        val result = client.getNow(BigDecimal("52.52"), BigDecimal("13.419998"))

        assertThat(result).isNull()
    }
}

