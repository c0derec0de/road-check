package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.common.domain.entities.Region
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.RegionRepository
import java.math.BigDecimal
import java.util.Optional

class RegionWeatherServiceTest {

    private val regionRepository: RegionRepository = mockk()
    private val externalWeatherClient: ExternalWeatherClient = mockk()

    private val service = RegionWeatherService(
        regionRepository = regionRepository,
        externalWeatherClient = externalWeatherClient,
    )

    @Test
    fun `getNow returns region weather from external client`() {
        val region = Region().apply {
            id = 1L
            regName = "Test region"
            centerLat = BigDecimal("52.52")
            centerLng = BigDecimal("13.419998")
        }
        every { regionRepository.findById(1L) } returns Optional.of(region)
        every { externalWeatherClient.getNow(any(), any()) } returns
            ExternalWeatherClient.ExternalWeatherResult(
                temperature = BigDecimal("7.0"),
                precipitation = BigDecimal("0.3"),
                windSpeedKmh = BigDecimal("8.9"),
                humidity = 75,
            )

        val result = service.getNow(1L)

        assertThat(result.regionId).isEqualTo(1L)
        assertThat(result.regionName).isEqualTo("Test region")
        assertThat(result.temperature).isEqualTo(7)
        assertThat(result.precipitationMm).isEqualByComparingTo("0.3")
        assertThat(result.windSpeedKmh).isEqualByComparingTo("8.9")
        assertThat(result.humidity).isEqualTo(75)
    }

    @Test
    fun `getNow throws when region not found`() {
        every { regionRepository.findById(42L) } returns Optional.empty()

        assertThatThrownBy { service.getNow(42L) }
            .isInstanceOf(NotFoundException::class.java)
    }
}

