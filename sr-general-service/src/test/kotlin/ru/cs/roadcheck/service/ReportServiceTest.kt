package ru.cs.roadcheck.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import ru.cs.roadcheck.blockchain.BlockchainRecordResult
import ru.cs.roadcheck.blockchain.BlockchainService
import ru.cs.roadcheck.common.domain.entities.Report
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.common.domain.entities.User
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.BotReportRequest
import ru.cs.roadcheck.rest.dto.ConfirmReportRequest
import ru.cs.roadcheck.rest.dto.CreateReportRequest
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class ReportServiceTest {

    private val reportRepository: ReportRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val blockchainService: BlockchainService = mockk()

    private lateinit var reportService: ReportService

    private val fixedInstant = Instant.parse("2024-06-15T12:00:00Z")

    @BeforeEach
    fun setUp() {
        reportService = ReportService(
            reportRepository = reportRepository,
            userRepository = userRepository,
            blockchainService = blockchainService,
        )
    }

    private fun reportWithAudit(
        id: Long = 1L,
        userId: Long = 1L,
        policeUserId: Long = userId,
        status: ReportStatus = ReportStatus.NEW,
    ): Report = Report().apply {
        this.id = id
        this.userId = userId
        this.policeUserId = policeUserId
        incidentType = "ДТП"
        latitude = BigDecimal("55.0")
        longitude = BigDecimal("37.0")
        description = "desc"
        this.status = status
        createdAt = fixedInstant
        updatedAt = fixedInstant
    }

    private fun userEntity(
        id: Long = 10L,
        login: String? = "login10",
        vkId: String? = null,
        email: String? = null,
        phone: String? = null,
    ): User = User().apply {
        this.id = id
        this.login = login
        this.vkId = vkId
        this.email = email
        this.phone = phone
    }

    /** JPA auditing does not run in unit tests; ReportService uses createdAt for blockchain hash and response. */
    private fun Report.ensureAuditAndId(newId: Long) {
        id = newId
        createdAt = fixedInstant
        updatedAt = fixedInstant
    }

    private fun baseCreateRequest(
        policeUserId: Long = 1L,
        userId: Long = 2L,
        incidentType: String = "ДТП",
    ) = CreateReportRequest(
        policeUserId = policeUserId,
        userId = userId,
        incidentType = incidentType,
        latitude = BigDecimal("55.0"),
        longitude = BigDecimal("37.0"),
        description = "x",
        photosUuid = null,
        fatalities = null,
        injuries = null,
        cause = null,
    )

    @Test
    fun `create throws when incident type blank`() {
        assertThatThrownBy {
            reportService.create(baseCreateRequest(incidentType = "   "), actingUserId = 1L, isModerator = false)
        }.isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("incident_type")
    }

    @Test
    fun `create as non-moderator forces userId to acting user`() {
        val req = baseCreateRequest(policeUserId = 1L, userId = 999L)
        val captured = slot<Report>()
        every { blockchainService.isAvailable() } returns false
        every { reportRepository.save(capture(captured)) } answers {
            captured.captured.ensureAuditAndId(50L)
            captured.captured
        }

        val resp = reportService.create(req, actingUserId = 5L, isModerator = false)

        assertThat(resp.userId).isEqualTo(5L)
        assertThat(captured.captured.userId).isEqualTo(5L)
    }

    @Test
    fun `create as moderator keeps request user ids`() {
        val req = baseCreateRequest(policeUserId = 1L, userId = 2L)
        every { blockchainService.isAvailable() } returns false
        every { reportRepository.save(any()) } answers {
            firstArg<Report>().apply { ensureAuditAndId(1L) }
        }

        val resp = reportService.create(req, actingUserId = 99L, isModerator = true)

        assertThat(resp.userId).isEqualTo(2L)
        verify { reportRepository.save(any()) }
    }

    @Test
    fun `create without blockchain saves once`() {
        val req = baseCreateRequest()
        every { blockchainService.isAvailable() } returns false
        every { reportRepository.save(any()) } answers {
            firstArg<Report>().apply { ensureAuditAndId(7L) }
        }

        reportService.create(req, actingUserId = 1L, isModerator = true)

        verify(exactly = 1) { reportRepository.save(any()) }
    }

    @Test
    fun `create with blockchain records and saves twice when RPC returns hash`() {
        val req = baseCreateRequest()
        every { blockchainService.isAvailable() } returns true
        every { blockchainService.recordReport(any(), any()) } returns BlockchainRecordResult("0xabc", 42L)
        every { reportRepository.save(any()) } answers {
            firstArg<Report>().apply {
                if (id == null) ensureAuditAndId(8L) else {
                    createdAt = fixedInstant
                    updatedAt = fixedInstant
                }
            }
        }

        val resp = reportService.create(req, actingUserId = 1L, isModerator = true)

        assertThat(resp.blockchainTxHash).isEqualTo("0xabc")
        verify(exactly = 2) { reportRepository.save(any()) }
        verify { blockchainService.recordReport(8L, any()) }
    }

    @Test
    fun `create with blockchain skips second save when recordReport returns null`() {
        val req = baseCreateRequest()
        every { blockchainService.isAvailable() } returns true
        every { blockchainService.recordReport(any(), any()) } returns null
        every { reportRepository.save(any()) } answers {
            firstArg<Report>().apply { ensureAuditAndId(9L) }
        }

        reportService.create(req, actingUserId = 1L, isModerator = true)

        verify(exactly = 1) { reportRepository.save(any()) }
    }

    @Test
    fun `createByBot throws when vkId blank`() {
        val req = BotReportRequest(
            vkId = "  ",
            incidentType = "ДТП",
            description = "d",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.ONE,
        )
        assertThatThrownBy { reportService.createByBot(req) }
            .isInstanceOf(ValidationException::class.java)
            .hasMessageContaining("vkId")
    }

    @Test
    fun `createByBot throws when user not found`() {
        every { userRepository.findByVkId("vk1") } returns null
        val req = BotReportRequest(
            vkId = "vk1",
            incidentType = "ДТП",
            description = "d",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.ONE,
        )
        assertThatThrownBy { reportService.createByBot(req) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `createByBot delegates to create with moderator`() {
        val u = userEntity(id = 3L, vkId = "vk3")
        every { userRepository.findByVkId("vk3") } returns u
        every { blockchainService.isAvailable() } returns false
        every { reportRepository.save(any()) } answers {
            firstArg<Report>().apply { ensureAuditAndId(100L) }
        }

        val req = BotReportRequest(
            vkId = "vk3",
            incidentType = "  Яма  ",
            description = "d",
            latitude = BigDecimal("55.1"),
            longitude = BigDecimal("37.2"),
            photoUrl = "p1",
        )
        val resp = reportService.createByBot(req)

        assertThat(resp.id).isEqualTo(100L)
        assertThat(resp.incidentType).isEqualTo("Яма")
        verify { reportRepository.save(any()) }
    }

    @Test
    fun `findAll excludes archived mapping`() {
        val r1 = reportWithAudit(id = 1L, status = ReportStatus.NEW)
        val r2 = reportWithAudit(id = 2L, status = ReportStatus.CONFIRMED)
        every { reportRepository.findAllByStatusNot(ReportStatus.ARCHIEVED) } returns listOf(r1, r2)

        val list = reportService.findAll()

        assertThat(list).hasSize(2)
        assertThat(list.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `findById returns response when not archived`() {
        val r = reportWithAudit(id = 5L)
        every { reportRepository.findById(5L) } returns Optional.of(r)

        val resp = reportService.findById(5L)

        assertThat(resp.id).isEqualTo(5L)
    }

    @Test
    fun `findById throws when missing`() {
        every { reportRepository.findById(1L) } returns Optional.empty()

        assertThatThrownBy { reportService.findById(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findById throws when archived`() {
        val r = reportWithAudit(id = 1L, status = ReportStatus.ARCHIEVED)
        every { reportRepository.findById(1L) } returns Optional.of(r)

        assertThatThrownBy { reportService.findById(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findList uses zero-based page from one-based input`() {
        val pageableSlot = slot<Pageable>()
        every {
            reportRepository.findByStatusAndRiskLevel(null, null, null, null, capture(pageableSlot))
        } returns PageImpl(emptyList())

        reportService.findList(page = 3, size = 15, status = null, riskLevel = null, regionId = null, scopeUserId = null)

        assertThat(pageableSlot.captured).isEqualTo(PageRequest.of(2, 15))
    }

    @Test
    fun `findList passes resolved status for valid filter`() {
        every {
            reportRepository.findByStatusAndRiskLevel("NEW", null, null, null, any())
        } returns PageImpl(emptyList())

        reportService.findList(1, 20, "new", null, null, null)

        verify { reportRepository.findByStatusAndRiskLevel("NEW", null, null, null, any()) }
    }

    @Test
    fun `findList passes null status when status string invalid`() {
        every {
            reportRepository.findByStatusAndRiskLevel(null, null, null, null, any())
        } returns PageImpl(emptyList())

        reportService.findList(1, 20, "NOT_A_STATUS", null, null, null)

        verify { reportRepository.findByStatusAndRiskLevel(null, null, null, null, any()) }
    }

    @Test
    fun `findList builds username from vk when login missing`() {
        val rep = reportWithAudit(id = 1L, userId = 77L)
        every {
            reportRepository.findByStatusAndRiskLevel(any(), any(), any(), any(), any())
        } returns PageImpl(listOf(rep))
        every { userRepository.findById(77L) } returns Optional.of(userEntity(id = 77L, login = null, vkId = "vk77"))

        val result = reportService.findList(1, 20, null, null, null, null)

        assertThat(result.reports).hasSize(1)
        assertThat(result.reports[0].username).isEqualTo("vk77")
        assertThat(result.filters.availableStatuses).doesNotContain("ARCHIEVED")
    }

    @Test
    fun `findDetailById allows moderator to view any non-archived`() {
        val r = reportWithAudit(id = 10L, userId = 2L)
        every { reportRepository.findById(10L) } returns Optional.of(r)
        every { userRepository.findById(2L) } returns Optional.of(userEntity(id = 2L))

        val detail = reportService.findDetailById(10L, viewerUserId = 99L, isModerator = true)

        assertThat(detail.id).isEqualTo(10L)
    }

    @Test
    fun `findDetailById allows owner when not moderator`() {
        val r = reportWithAudit(id = 10L, userId = 5L)
        every { reportRepository.findById(10L) } returns Optional.of(r)
        every { userRepository.findById(5L) } returns Optional.of(userEntity(id = 5L))

        val detail = reportService.findDetailById(10L, viewerUserId = 5L, isModerator = false)

        assertThat(detail.id).isEqualTo(10L)
    }

    @Test
    fun `findDetailById denies non-owner non-moderator`() {
        val r = reportWithAudit(id = 10L, userId = 2L)
        every { reportRepository.findById(10L) } returns Optional.of(r)

        assertThatThrownBy {
            reportService.findDetailById(10L, viewerUserId = 3L, isModerator = false)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findDetailById throws when archived`() {
        val r = reportWithAudit(id = 1L, status = ReportStatus.ARCHIEVED)
        every { reportRepository.findById(1L) } returns Optional.of(r)

        assertThatThrownBy {
            reportService.findDetailById(1L, viewerUserId = 1L, isModerator = true)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findByVkId throws when blank`() {
        assertThatThrownBy { reportService.findByVkId("  ", 1, 20) }
            .isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `findByVkId throws when user missing`() {
        every { userRepository.findByVkId("x") } returns null

        assertThatThrownBy { reportService.findByVkId("x", 1, 20) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findByVkId scopes list to user id`() {
        val u = userEntity(id = 44L, vkId = "vk44")
        every { userRepository.findByVkId("vk44") } returns u
        every {
            reportRepository.findByStatusAndRiskLevel(null, null, null, 44L, any())
        } returns PageImpl(emptyList())

        reportService.findByVkId("vk44", page = 1, size = 10)

        verify { reportRepository.findByStatusAndRiskLevel(null, null, null, 44L, any()) }
    }

    @Test
    fun `findDetailByIdForBot throws when not found`() {
        every { reportRepository.findById(1L) } returns Optional.empty()

        assertThatThrownBy { reportService.findDetailByIdForBot(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findDetailByIdForBot throws when archived`() {
        val r = reportWithAudit(id = 1L, status = ReportStatus.ARCHIEVED)
        every { reportRepository.findById(1L) } returns Optional.of(r)

        assertThatThrownBy { reportService.findDetailByIdForBot(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `findDetailByIdForBot returns detail`() {
        val r = reportWithAudit(id = 3L, userId = 8L)
        every { reportRepository.findById(3L) } returns Optional.of(r)
        every { userRepository.findById(8L) } returns Optional.of(userEntity(id = 8L))

        val d = reportService.findDetailByIdForBot(3L)

        assertThat(d.id).isEqualTo(3L)
    }

    @Test
    fun `confirm throws when comment blank`() {
        assertThatThrownBy {
            reportService.confirm(1L, ConfirmReportRequest(comment = "  "))
        }.isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `confirm throws when report missing`() {
        every { reportRepository.findById(1L) } returns Optional.empty()

        assertThatThrownBy {
            reportService.confirm(1L, ConfirmReportRequest(comment = "ok"))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `confirm throws when archived`() {
        val r = reportWithAudit(id = 1L, status = ReportStatus.ARCHIEVED)
        every { reportRepository.findById(1L) } returns Optional.of(r)

        assertThatThrownBy {
            reportService.confirm(1L, ConfirmReportRequest(comment = "ok"))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `confirm updates status and persists`() {
        val r = reportWithAudit(id = 1L)
        every { reportRepository.findById(1L) } returns Optional.of(r)
        every { reportRepository.save(any()) } answers { firstArg() }

        val out = reportService.confirm(1L, ConfirmReportRequest(comment = " Принято "))

        assertThat(out.status).isEqualTo("CONFIRMED")
        assertThat(r.comment).isEqualTo("Принято")
        verify { reportRepository.save(r) }
    }

    @Test
    fun `decline throws when not found`() {
        every { reportRepository.findById(1L) } returns Optional.empty()

        assertThatThrownBy { reportService.decline(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `decline throws when archived`() {
        val r = reportWithAudit(id = 1L, status = ReportStatus.ARCHIEVED)
        every { reportRepository.findById(1L) } returns Optional.of(r)

        assertThatThrownBy { reportService.decline(1L) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `decline sets declined and clears comment`() {
        val r = reportWithAudit(id = 1L)
        r.comment = "old"
        every { reportRepository.findById(1L) } returns Optional.of(r)
        every { reportRepository.save(any()) } answers { firstArg() }

        val out = reportService.decline(1L)

        assertThat(out.status).isEqualTo("DECLINED")
        assertThat(r.comment).isNull()
    }

    @Test
    fun `archiveProcessedReports returns repository count`() {
        every { reportRepository.archiveProcessedReports() } returns 12

        assertThat(reportService.archiveProcessedReports()).isEqualTo(12)
    }
}
