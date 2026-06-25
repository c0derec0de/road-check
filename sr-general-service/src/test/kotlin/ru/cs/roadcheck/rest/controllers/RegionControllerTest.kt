package ru.cs.roadcheck.rest.controllers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.cs.roadcheck.rest.dto.RegionResponse
import ru.cs.roadcheck.rest.dto.RegionWeatherResponse
import ru.cs.roadcheck.service.RegionService
import ru.cs.roadcheck.service.RegionWeatherService
import java.math.BigDecimal

class RegionControllerTest {

    private val regionService: RegionService = mockk()
    private val regionWeatherService: RegionWeatherService = mockk()
    private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            RegionController(regionService, regionWeatherService),
        ).build()
    }

    @Test
    fun `list regions`() {
        every { regionService.findAll() } returns listOf(
            RegionResponse(1L, "77", "Москва", BigDecimal.ONE, BigDecimal.ONE),
        )

        mockMvc.perform(get("/api/regions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))

        verify { regionService.findAll() }
    }

    @Test
    fun `get region by id`() {
        every { regionService.findById(2L) } returns RegionResponse(2L, "78", "СПб", null, null)

        mockMvc.perform(get("/api/regions/2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.regName").value("СПб"))
    }

    @Test
    fun `get weather`() {
        every { regionWeatherService.getNow(1L) } returns RegionWeatherResponse(
            regionId = 1L,
            regionName = "Москва",
            temperature = 5,
            precipitationMm = BigDecimal.ZERO,
            windSpeedKmh = BigDecimal.TEN,
            humidity = 50,
        )

        mockMvc.perform(get("/api/regions/1/weather"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.temperature").value(5))
    }
}
