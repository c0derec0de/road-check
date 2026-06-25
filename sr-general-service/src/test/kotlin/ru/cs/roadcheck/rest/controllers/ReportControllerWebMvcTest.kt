package ru.cs.roadcheck.rest.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.cs.roadcheck.rest.GlobalExceptionHandler
import ru.cs.roadcheck.rest.dto.ConfirmReportRequest
import ru.cs.roadcheck.rest.dto.CreateReportRequest
import ru.cs.roadcheck.rest.dto.FiltersResponse
import ru.cs.roadcheck.rest.dto.PaginationResponse
import ru.cs.roadcheck.rest.dto.ReportDetailResponse
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.ReportsListResponse
import ru.cs.roadcheck.service.ReportService
import java.math.BigDecimal
import java.time.Instant

class ReportControllerWebMvcTest {

    private val reportService: ReportService = mockk()
    private lateinit var controller: ReportController
    private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var objectMapper: ObjectMapper

    private fun userAuth(id: Long) = UsernamePasswordAuthenticationToken(
        id,
        null,
        listOf(SimpleGrantedAuthority("ROLE_USER")),
    )

    private fun modAuth(id: Long) = UsernamePasswordAuthenticationToken(
        id,
        null,
        listOf(SimpleGrantedAuthority("ROLE_MODERATOR")),
    )

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        controller = ReportController(reportService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `create report as user`() {
        val body = CreateReportRequest(
            policeUserId = 1L,
            userId = 2L,
            incidentType = "ДТП",
            latitude = BigDecimal("55.0"),
            longitude = BigDecimal("37.0"),
            description = "test",
            photosUuid = null,
            fatalities = null,
            injuries = null,
            cause = null,
        )
        val resp = ReportResponse(
            id = 1L,
            policeUserId = 1L,
            userId = 2L,
            incidentType = "ДТП",
            latitude = body.latitude,
            longitude = body.longitude,
            description = "test",
            comment = null,
            photosUuid = null,
            status = "NEW",
            createdAt = Instant.now(),
            blockchainTxHash = null,
            fatalities = null,
            injuries = null,
            cause = null,
        )
        every { reportService.create(any(), 2L, false) } returns resp

        val result = controller.create(userAuth(2L), body)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
        verify { reportService.create(any(), 2L, false) }
    }

    @Test
    fun `list reports as user scopes to own id`() {
        val empty = ReportsListResponse(
            reports = emptyList(),
            pagination = PaginationResponse(1, 0, 0L, 20),
            filters = FiltersResponse(
                availableStatuses = listOf("NEW"),
                availableRiskLevels = listOf("high"),
            ),
        )
        every { reportService.findList(1, 20, null, null, null, 2L) } returns empty

        val result = controller.list(userAuth(2L), 1, 20, null, null, null)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
        verify { reportService.findList(1, 20, null, null, null, 2L) }
    }

    @Test
    fun `list reports as moderator uses full list`() {
        val empty = ReportsListResponse(
            reports = emptyList(),
            pagination = PaginationResponse(1, 0, 0L, 20),
            filters = FiltersResponse(
                availableStatuses = listOf("NEW"),
                availableRiskLevels = listOf("high"),
            ),
        )
        every { reportService.findList(1, 20, null, null, null, null) } returns empty

        val result = controller.list(modAuth(1L), 1, 20, null, null, null)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
        verify { reportService.findList(1, 20, null, null, null, null) }
    }

    @Test
    fun `getById delegates`() {
        val detail = ReportDetailResponse(
            id = 9L,
            title = "t",
            address = "a",
            description = "d",
            comment = null,
            status = "NEW",
            riskLevel = null,
            isDangerousZone = false,
            user = null,
            createdAt = Instant.now(),
            updatedAt = null,
            photos = emptyList(),
            location = ru.cs.roadcheck.rest.dto.LocationResponse(lat = BigDecimal.ZERO, lng = BigDecimal.ZERO),
            blockchainTxHash = null,
            blockchainVerified = false,
            blockchainBlockNumber = null,
            comments = emptyList(),
        )
        every { reportService.findDetailById(9L, 2L, false) } returns detail

        val result = controller.getById(userAuth(2L), 9L)

        assertThat(result.statusCode.is2xxSuccessful).isTrue()
        assertThat(result.body?.id).isEqualTo(9L)
    }

    @Test
    fun `confirm delegates to service`() {
        every { reportService.confirm(3L, ConfirmReportRequest("ok")) } returns ReportResponse(
            id = 3L,
            policeUserId = 1L,
            userId = 2L,
            incidentType = "x",
            latitude = null,
            longitude = null,
            description = null,
            comment = "ok",
            photosUuid = null,
            status = "CONFIRMED",
            createdAt = Instant.now(),
            blockchainTxHash = null,
            fatalities = null,
            injuries = null,
            cause = null,
        )

        mockMvc.perform(
            put("/api/reports/3/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ConfirmReportRequest("ok"))),
        )
            .andExpect(status().isOk)

        verify { reportService.confirm(3L, ConfirmReportRequest("ok")) }
    }

    @Test
    fun `decline delegates to service`() {
        every { reportService.decline(4L) } returns ReportResponse(
            id = 4L,
            policeUserId = 1L,
            userId = 2L,
            incidentType = "x",
            latitude = null,
            longitude = null,
            description = null,
            comment = null,
            photosUuid = null,
            status = "DECLINED",
            createdAt = Instant.now(),
            blockchainTxHash = null,
            fatalities = null,
            injuries = null,
            cause = null,
        )

        mockMvc.perform(put("/api/reports/4/decline"))
            .andExpect(status().isOk)
    }
}
