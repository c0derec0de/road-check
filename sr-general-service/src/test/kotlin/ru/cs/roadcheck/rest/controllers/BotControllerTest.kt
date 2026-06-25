 package ru.cs.roadcheck.rest.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.cs.roadcheck.rest.GlobalExceptionHandler
import ru.cs.roadcheck.rest.dto.BotReportsDetailListResponse
import ru.cs.roadcheck.rest.dto.BotRegisterRequest
import ru.cs.roadcheck.rest.dto.BotReportRequest
import ru.cs.roadcheck.rest.dto.LocationResponse
import ru.cs.roadcheck.rest.dto.ReportDetailResponse
import ru.cs.roadcheck.rest.dto.RegisterResponse
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.ReportsListResponse
import ru.cs.roadcheck.config.BotProperties
import ru.cs.roadcheck.service.AuthService
import ru.cs.roadcheck.service.ReportService
import java.math.BigDecimal

class BotControllerTest {

    private val authService: AuthService = mockk()
    private val reportService: ReportService = mockk()
    private val botProperties: BotProperties = mockk()

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    private val validApiToken = "X-API-TOKEN"

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        every { botProperties.apiToken } returns validApiToken

        val controller = BotController(
            authService = authService,
            reportService = reportService,
            botProperties = botProperties
        )
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `register with valid token and data returns created`() {
        val request = BotRegisterRequest(
            login = "bot_user_123",
            password = "password123",
            email = "user@example.com",
            vkId = "12345678",
            firstname = "Test",
            lastname = "User",
            phone = "+79991234567",
        )

        val response = RegisterResponse(
            success = true,
            message = "Регистрация через бота успешна",
            userId = 1L,
            token = "jwt-token",
            blockchainVerified = false,
            role = "USER",
        )

        every { authService.registerByBot(any()) } returns response

        mockMvc.perform(
            post("/api/internal/bot/register")
                .header("X-API-TOKEN", validApiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.token").value("jwt-token"))

        verify { authService.registerByBot(any()) }
    }

    @Test
    fun `register without token returns bad request`() {
        val request = BotRegisterRequest(
            login = "bot_user_123",
            password = "password123",
            email = "user@example.com",
            vkId = "12345678",
            phone = "+79991234567",
        )

        mockMvc.perform(
            post("/api/internal/bot/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("X-API-TOKEN не может быть пустым"))
    }

    @Test
    fun `register with invalid token returns bad request`() {
        val request = BotRegisterRequest(
            login = "bot_user_123",
            password = "password123",
            email = "user@example.com",
            vkId = "12345678",
            phone = "+79991234567",
        )

        mockMvc.perform(
            post("/api/internal/bot/register")
                .header("X-API-TOKEN", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Неверный X-API-TOKEN"))
    }

    @Test
    fun `createReport with valid token and data returns created`() {
        val request = BotReportRequest(
            vkId = "12345678",
            incidentType = "Повреждение дорожного покрытия",
            description = "Большая яма на проезжей части",
            latitude = BigDecimal("55.755826"),
            longitude = BigDecimal("37.617299"),
            regionId = 1L,
            photoUrl = "https://example.com/photo.jpg",
        )

        val response = ReportResponse(
            id = 1L,
            policeUserId = 1L,
            userId = 1L,
            incidentType = "Повреждение дорожного покрытия",
            description = "Большая яма на проезжей части",
            latitude = BigDecimal("55.755826"),
            longitude = BigDecimal("37.617299"),
            comment = null,
            photosUuid = "https://example.com/photo.jpg",
            status = "NEW",
            createdAt = java.time.Instant.now(),
            blockchainTxHash = null,
            fatalities = null,
            injuries = null,
            cause = null,
        )

        every { reportService.createByBot(any()) } returns response

        mockMvc.perform(
            post("/api/internal/bot/reports")
                .header("X-API-TOKEN", validApiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.incidentType").value("Повреждение дорожного покрытия"))

        verify { reportService.createByBot(any()) }
    }

    @Test
    fun `createReport without token returns bad request`() {
        val request = BotReportRequest(
            vkId = "12345678",
            incidentType = "Повреждение дорожного покрытия",
            description = "Большая яма",
            latitude = BigDecimal("55.755826"),
            longitude = BigDecimal("37.617299"),
        )

        mockMvc.perform(
            post("/api/internal/bot/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getReports with valid token returns reports list`() {
        val reportsList = ReportsListResponse(
            reports = emptyList(),
            pagination = ru.cs.roadcheck.rest.dto.PaginationResponse(
                currentPage = 1,
                totalPages = 0,
                totalItems = 0,
                itemsPerPage = 20,
            ),
            filters = ru.cs.roadcheck.rest.dto.FiltersResponse(
                availableStatuses = listOf("NEW", "IN_PROGRESS", "CONFIRMED"),
                availableRiskLevels = listOf("high", "medium", "low"),
            ),
        )

        every { reportService.findByVkId("12345678", 1, 20) } returns reportsList

        mockMvc.perform(
            get("/api/internal/bot/reports")
                .header("X-API-TOKEN", validApiToken)
                .param("vkId", "12345678")
                .param("page", "1")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reports").isArray)
            .andExpect(jsonPath("$.pagination.currentPage").value(1))

        verify { reportService.findByVkId("12345678", 1, 20) }
    }

    @Test
    fun `getAllReportsDetailed returns list`() {
        val response = BotReportsDetailListResponse(
            reports = listOf(
                ReportDetailResponse(
                    id = 7L,
                    title = "Первый",
                    address = null,
                    description = "Описание",
                    comment = null,
                    status = "NEW",
                    riskLevel = null,
                    isDangerousZone = false,
                    user = null,
                    createdAt = java.time.Instant.now(),
                    updatedAt = null,
                    photos = emptyList(),
                    location = null,
                    blockchainTxHash = null,
                    blockchainVerified = false,
                    blockchainBlockNumber = null,
                    comments = emptyList(),
                ),
            ),
            total = 1,
        )
        every { reportService.findAllDetailsByVkIdForBot("999") } returns response

        mockMvc.perform(
            get("/api/internal/bot/reports/detailed-all")
                .header("X-API-TOKEN", validApiToken)
                .param("vkId", "999")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.reports[0].id").value(7))

        verify { reportService.findAllDetailsByVkIdForBot("999") }
    }

    @Test
    fun `getReports with pagination parameters`() {
        val reportsList = ReportsListResponse(
            reports = emptyList(),
            pagination = ru.cs.roadcheck.rest.dto.PaginationResponse(
                currentPage = 2,
                totalPages = 5,
                totalItems = 100,
                itemsPerPage = 10,
            ),
            filters = ru.cs.roadcheck.rest.dto.FiltersResponse(
                availableStatuses = listOf("NEW"),
                availableRiskLevels = listOf("high"),
            ),
        )

        every { reportService.findByVkId("12345678", 2, 10) } returns reportsList

        mockMvc.perform(
            get("/api/internal/bot/reports")
                .header("X-API-TOKEN", validApiToken)
                .param("vkId", "12345678")
                .param("page", "2")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pagination.currentPage").value(2))
            .andExpect(jsonPath("$.pagination.totalPages").value(5))

        verify { reportService.findByVkId("12345678", 2, 10) }
    }

    @Test
    fun `getReportDetail with valid token returns report`() {
        val response = ReportDetailResponse(
            id = 42L,
            title = "Повреждение дорожного покрытия",
            address = "ул. Пушкина, д. 1",
            description = "Большая яма",
            comment = null,
            status = "NEW",
            riskLevel = null,
            isDangerousZone = false,
            user = null,
            createdAt = java.time.Instant.now(),
            updatedAt = null,
            photos = emptyList(),
            location = LocationResponse(BigDecimal("55.755826"), BigDecimal("37.617299")),
            blockchainTxHash = null,
            blockchainVerified = false,
            blockchainBlockNumber = null,
            comments = emptyList(),
        )

        every { reportService.findDetailByIdForBot(42L) } returns response

        mockMvc.perform(
            get("/api/internal/bot/reports/42")
                .header("X-API-TOKEN", validApiToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.title").value("Повреждение дорожного покрытия"))

        verify { reportService.findDetailByIdForBot(42L) }
    }

    @Test
    fun `getReportDetail without token returns bad request`() {
        mockMvc.perform(
            get("/api/internal/bot/reports/42")
        )
            .andExpect(status().isBadRequest)
    }
}
