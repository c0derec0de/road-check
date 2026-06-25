package ru.cs.roadcheck.rest.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.cs.roadcheck.rest.controllers.manager.ManagerDangerousZoneController
import ru.cs.roadcheck.rest.controllers.manager.ManagerRegionController
import ru.cs.roadcheck.rest.controllers.manager.ManagerReportController
import ru.cs.roadcheck.rest.controllers.manager.ManagerRoadController
import ru.cs.roadcheck.rest.controllers.manager.ManagerUserController
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.manager.DangerousZoneManagerRequest
import ru.cs.roadcheck.rest.dto.manager.DangerousZoneManagerResponse
import ru.cs.roadcheck.rest.dto.manager.RegionManagerRequest
import ru.cs.roadcheck.rest.dto.manager.RegionManagerResponse
import ru.cs.roadcheck.rest.dto.manager.ReportManagerRequest
import ru.cs.roadcheck.rest.dto.manager.RoadManagerRequest
import ru.cs.roadcheck.rest.dto.manager.RoadManagerResponse
import ru.cs.roadcheck.rest.dto.manager.UserManagerRequest
import ru.cs.roadcheck.rest.dto.manager.UserManagerResponse
import ru.cs.roadcheck.service.manager.DangerousZoneManagerService
import ru.cs.roadcheck.service.manager.RegionManagerService
import ru.cs.roadcheck.service.manager.ReportManagerService
import ru.cs.roadcheck.service.manager.RoadManagerService
import ru.cs.roadcheck.service.manager.UserManagerService
import java.math.BigDecimal
import java.time.Instant

class ManagerControllersWebMvcTest {

    private val userManagerService: UserManagerService = mockk()
    private val regionManagerService: RegionManagerService = mockk()
    private val reportManagerService: ReportManagerService = mockk()
    private val roadManagerService: RoadManagerService = mockk()
    private val dangerousZoneManagerService: DangerousZoneManagerService = mockk()

    private lateinit var usersMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var regionsMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var reportsMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var roadsMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var zonesMvc: org.springframework.test.web.servlet.MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        usersMvc = MockMvcBuilders.standaloneSetup(ManagerUserController(userManagerService)).build()
        regionsMvc = MockMvcBuilders.standaloneSetup(ManagerRegionController(regionManagerService)).build()
        reportsMvc = MockMvcBuilders.standaloneSetup(ManagerReportController(reportManagerService)).build()
        roadsMvc = MockMvcBuilders.standaloneSetup(ManagerRoadController(roadManagerService)).build()
        zonesMvc = MockMvcBuilders.standaloneSetup(ManagerDangerousZoneController(dangerousZoneManagerService)).build()
    }

    private fun sampleUserResponse(id: Long) = UserManagerResponse(
        id = id,
        role = "USER",
        login = "a",
        vkId = null,
        firstname = null,
        middlename = null,
        lastname = null,
        department = null,
        city = null,
        phone = null,
        email = "a@b.co",
        walletAddress = null,
        blockchainVerified = false,
    )

    private fun sampleReportManagerRequest() = ReportManagerRequest(
        policeUserId = 1L,
        userId = 2L,
        incidentType = "ДТП",
        latitude = null,
        longitude = null,
        description = null,
        photosUuid = null,
        status = "NEW",
        fatalities = null,
        injuries = null,
        cause = null,
        riskLevel = null,
        title = null,
        address = null,
        isDangerousZone = null,
    )

    private fun sampleReportResponse(id: Long) = ReportResponse(
        id = id,
        policeUserId = 1L,
        userId = 2L,
        incidentType = "ДТП",
        latitude = null,
        longitude = null,
        description = null,
        comment = null,
        photosUuid = null,
        status = "NEW",
        createdAt = Instant.now(),
        blockchainTxHash = null,
        fatalities = null,
        injuries = null,
        cause = null,
    )

    @Test
    fun `manager users crud smoke`() {
        every { userManagerService.findAll() } returns emptyList()
        usersMvc.perform(get("/api/manager/users")).andExpect(status().isOk)

        every { userManagerService.findById(1L) } returns sampleUserResponse(1L)
        usersMvc.perform(get("/api/manager/users/1")).andExpect(status().isOk)

        val req = UserManagerRequest(
            login = "a",
            vkId = null,
            firstname = null,
            middlename = null,
            lastname = null,
            department = null,
            city = null,
            phone = null,
            email = "a@b.co",
            walletAddress = null,
        )
        every { userManagerService.create(any()) } returns sampleUserResponse(2L)
        usersMvc.perform(
            post("/api/manager/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isCreated)

        every { userManagerService.update(1L, any()) } returns sampleUserResponse(1L)
        usersMvc.perform(
            put("/api/manager/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isOk)

        every { userManagerService.delete(1L) } just runs
        usersMvc.perform(delete("/api/manager/users/1")).andExpect(status().isNoContent)
    }

    @Test
    fun `manager regions smoke`() {
        every { regionManagerService.findAll() } returns emptyList()
        regionsMvc.perform(get("/api/manager/regions")).andExpect(status().isOk)

        every { regionManagerService.findById(1L) } returns RegionManagerResponse(1L, "77", "Москва", null, null)
        regionsMvc.perform(get("/api/manager/regions/1")).andExpect(status().isOk)

        val req = RegionManagerRequest("77", "Москва", BigDecimal.ONE, BigDecimal.ONE)
        every { regionManagerService.create(any()) } returns RegionManagerResponse(1L, "77", "Москва", BigDecimal.ONE, BigDecimal.ONE)
        regionsMvc.perform(
            post("/api/manager/regions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isCreated)

        every { regionManagerService.update(1L, any()) } returns RegionManagerResponse(1L, "77", "Москва", null, null)
        regionsMvc.perform(
            put("/api/manager/regions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isOk)

        every { regionManagerService.delete(1L) } just runs
        regionsMvc.perform(delete("/api/manager/regions/1")).andExpect(status().isNoContent)
    }

    @Test
    fun `manager reports smoke`() {
        every { reportManagerService.findAll() } returns emptyList()
        reportsMvc.perform(get("/api/manager/reports")).andExpect(status().isOk)

        every { reportManagerService.findById(1L) } returns sampleReportResponse(1L)
        reportsMvc.perform(get("/api/manager/reports/1")).andExpect(status().isOk)

        val req = sampleReportManagerRequest()
        every { reportManagerService.create(any()) } returns sampleReportResponse(3L)
        reportsMvc.perform(
            post("/api/manager/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isCreated)

        every { reportManagerService.update(1L, any()) } returns sampleReportResponse(1L)
        reportsMvc.perform(
            put("/api/manager/reports/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isOk)

        every { reportManagerService.delete(1L) } just runs
        reportsMvc.perform(delete("/api/manager/reports/1")).andExpect(status().isNoContent)
    }

    @Test
    fun `manager roads smoke`() {
        every { roadManagerService.findAll() } returns emptyList()
        roadsMvc.perform(get("/api/manager/roads")).andExpect(status().isOk)

        every { roadManagerService.findById(1L) } returns RoadManagerResponse(1L, "R1")
        roadsMvc.perform(get("/api/manager/roads/1")).andExpect(status().isOk)

        val req = RoadManagerRequest("R1")
        every { roadManagerService.create(any()) } returns RoadManagerResponse(1L, "R1")
        roadsMvc.perform(
            post("/api/manager/roads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isCreated)

        every { roadManagerService.update(1L, any()) } returns RoadManagerResponse(1L, "R1")
        roadsMvc.perform(
            put("/api/manager/roads/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isOk)

        every { roadManagerService.delete(1L) } just runs
        roadsMvc.perform(delete("/api/manager/roads/1")).andExpect(status().isNoContent)
    }

    @Test
    fun `manager dangerous zones smoke`() {
        every { dangerousZoneManagerService.findAll() } returns emptyList()
        zonesMvc.perform(get("/api/manager/dangerous-zones")).andExpect(status().isOk)

        every { dangerousZoneManagerService.findById(1L) } returns DangerousZoneManagerResponse(
            id = 1L,
            name = "z",
            centerLat = BigDecimal.ZERO,
            centerLng = BigDecimal.ZERO,
            radius = 100,
            incidentsCount = 0,
            riskLevel = "high",
            isActive = true,
            regionId = 1L,
        )
        zonesMvc.perform(get("/api/manager/dangerous-zones/1")).andExpect(status().isOk)

        val req = DangerousZoneManagerRequest(
            name = "z",
            centerLat = BigDecimal.ZERO,
            centerLng = BigDecimal.ZERO,
            radius = 100,
            incidentsCount = 0,
            riskLevel = "high",
            isActive = true,
            regionId = 1L,
        )
        every { dangerousZoneManagerService.create(any()) } returns DangerousZoneManagerResponse(
            id = 2L,
            name = "z",
            centerLat = BigDecimal.ZERO,
            centerLng = BigDecimal.ZERO,
            radius = 100,
            incidentsCount = 0,
            riskLevel = "high",
            isActive = true,
            regionId = 1L,
        )
        zonesMvc.perform(
            post("/api/manager/dangerous-zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isCreated)

        every { dangerousZoneManagerService.update(1L, any()) } returns DangerousZoneManagerResponse(
            id = 1L,
            name = "z",
            centerLat = BigDecimal.ZERO,
            centerLng = BigDecimal.ZERO,
            radius = 100,
            incidentsCount = 0,
            riskLevel = "high",
            isActive = true,
            regionId = 1L,
        )
        zonesMvc.perform(
            put("/api/manager/dangerous-zones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)),
        ).andExpect(status().isOk)

        every { dangerousZoneManagerService.delete(1L) } just runs
        zonesMvc.perform(delete("/api/manager/dangerous-zones/1")).andExpect(status().isNoContent)
    }
}
