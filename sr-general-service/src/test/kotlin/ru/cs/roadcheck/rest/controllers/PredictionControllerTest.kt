package ru.cs.roadcheck.rest.controllers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.quartz.JobKey
import org.quartz.Scheduler
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.hamcrest.Matchers.containsString
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.cs.roadcheck.service.SvgChartService

class PredictionControllerTest {

    private val scheduler: Scheduler = mockk()
    private val svgChartService: SvgChartService = mockk()
    private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            PredictionController(scheduler, svgChartService),
        ).build()
    }

    @Test
    fun `run manual triggers job`() {
        every { scheduler.triggerJob(JobKey("predictionJob", "predictionGroup")) } returns Unit

        mockMvc.perform(post("/api/predictions/run-manual"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("triggered")))

        verify { scheduler.triggerJob(JobKey("predictionJob", "predictionGroup")) }
    }

    @Test
    fun `monthly chart returns svg`() {
        every { svgChartService.generateMonthlyAccidentsChart() } returns "<svg></svg>"

        mockMvc.perform(get("/api/predictions/charts/monthly"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/svg+xml"))
    }

    @Test
    fun `risk chart returns svg`() {
        every { svgChartService.generateRiskDistributionChart() } returns "<svg>x</svg>"

        mockMvc.perform(get("/api/predictions/charts/risk"))
            .andExpect(status().isOk)
    }

    @Test
    fun `causes chart returns svg`() {
        every { svgChartService.generateCausesDistributionChart() } returns "<svg>y</svg>"

        mockMvc.perform(get("/api/predictions/charts/causes"))
            .andExpect(status().isOk)
    }
}
