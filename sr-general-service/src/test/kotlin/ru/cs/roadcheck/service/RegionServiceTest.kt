package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.common.domain.entities.Region
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.RegionRepository
import java.util.Optional

class RegionServiceTest {

    private val regionRepository: RegionRepository = mockk()

    private lateinit var regionService: RegionService

    @BeforeEach
    fun setUp() {
        regionService = RegionService(regionRepository)
    }

    @Test
    fun `findAll returns all regions`() {
        val regions = listOf(
            Region().apply { id = 1L; regName = "Region 1" },
            Region().apply { id = 2L; regName = "Region 2" },
        )
        every { regionRepository.findAll() } returns regions

        val result = regionService.findAll()

        assertThat(result).hasSize(2)
        assertThat(result[0].regName).isEqualTo("Region 1")
        assertThat(result[1].regName).isEqualTo("Region 2")
    }

    @Test
    fun `findAll returns empty list when no regions`() {
        every { regionRepository.findAll() } returns emptyList()

        val result = regionService.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findById returns region when found`() {
        val region = Region().apply {
            id = 1L
            regName = "Test Region"
        }
        every { regionRepository.findById(1L) } returns Optional.of(region)

        val result = regionService.findById(1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.regName).isEqualTo("Test Region")
    }

    @Test
    fun `findById throws when region not found`() {
        every { regionRepository.findById(42L) } returns Optional.empty()

        assertThatThrownBy { regionService.findById(42L) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Регион с id=42 не найден")
    }
}
