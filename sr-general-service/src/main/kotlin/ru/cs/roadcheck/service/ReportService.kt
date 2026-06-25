package ru.cs.roadcheck.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.blockchain.BlockchainService
import ru.cs.roadcheck.common.domain.entities.Report
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.CreateReportRequest
import ru.cs.roadcheck.rest.dto.BotReportRequest
import ru.cs.roadcheck.rest.dto.ConfirmReportRequest
import ru.cs.roadcheck.rest.dto.ReportDetailResponse
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.FiltersResponse
import ru.cs.roadcheck.rest.dto.PaginationResponse
import ru.cs.roadcheck.rest.dto.BotReportsDetailListResponse
import ru.cs.roadcheck.rest.dto.ReportsListResponse
import ru.cs.roadcheck.rest.dto.manager.toReport
import ru.cs.roadcheck.rest.dto.manager.toReportDetailResponse
import ru.cs.roadcheck.rest.dto.manager.toReportListItemResponse
import ru.cs.roadcheck.rest.dto.manager.toReportResponse
import java.security.MessageDigest
import java.time.format.DateTimeFormatter


@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val blockchainService: BlockchainService,
) {

    @Transactional
    fun createByBot(request: BotReportRequest): ReportResponse {
        val vkId = request.vkId.trim()
        if (vkId.isBlank()) throw ValidationException("vkId не может быть пустым")
        val user = userRepository.findByVkId(vkId)
            ?: throw NotFoundException("Пользователь с vkId=$vkId не найден")

        val createRequest = CreateReportRequest(
            policeUserId = user.id!!,
            userId = user.id!!,
            incidentType = request.incidentType,
            latitude = request.latitude,
            longitude = request.longitude,
            description = request.description,
            photosUuid = request.photoUrl,
            fatalities = null,
            injuries = null,
            cause = null,
        )
        return create(createRequest, user.id!!, isModerator = true)
    }

    @Transactional
    fun create(request: CreateReportRequest, actingUserId: Long, isModerator: Boolean): ReportResponse {
        if (request.incidentType.trim().isBlank()) throw ValidationException("incident_type не может быть пустым")
        val effective = if (isModerator) request else request.copy(userId = actingUserId)
        var saved = reportRepository.save(effective.toReport())
        if (blockchainService.isAvailable()) {
            val contentHash = sha256("${saved.id}|${saved.incidentType}|${saved.latitude}|${saved.longitude}|${saved.description}|${saved.createdAt}")
            blockchainService.recordReport(saved.id!!, contentHash)?.let { result ->
                saved.blockchainTxHash = result.transactionHash
                saved.blockchainVerified = true
                saved.blockchainBlockNumber = result.blockNumber
                saved = reportRepository.save(saved)
            }
        }
        return saved.toReportResponse()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun findAll() = reportRepository.findAllByStatusNot(ReportStatus.ARCHIEVED).map { it.toReportResponse() }

    fun findById(id: Long) = reportRepository.findById(id)
        .orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        .let {
            if (it.status == ReportStatus.ARCHIEVED) throw NotFoundException("Отчёт с id=$id не найден")
            it.toReportResponse()
        }

    fun findList(
        page: Int,
        size: Int,
        status: String?,
        riskLevel: String?,
        regionId: Long?,
        scopeUserId: Long?,
    ): ReportsListResponse {
        val statusEnum = status?.trim()?.uppercase()?.let { s ->
            try { ReportStatus.valueOf(s) } catch (e: IllegalArgumentException) { null }
        }
        val statusStr = statusEnum?.name
        val pageable: Pageable = PageRequest.of(page - 1, size)
        val reportsPage: Page<Report> = reportRepository.findByStatusAndRiskLevel(statusStr, riskLevel, regionId, scopeUserId, pageable)
        val reports = reportsPage.content.map { report ->
            val user = report.userId?.let { userRepository.findById(it).orElse(null) }
            val username = user?.login ?: user?.vkId ?: user?.email ?: user?.phone
            val date = report.createdAt?.atZone(java.time.ZoneId.of("Europe/Moscow"))?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            report.toReportListItemResponse(username, date)
        }

        return ReportsListResponse(
            reports = reports,
            pagination = PaginationResponse(
                currentPage = page,
                totalPages = reportsPage.totalPages,
                totalItems = reportsPage.totalElements,
                itemsPerPage = size,
            ),
            filters = FiltersResponse(
                availableStatuses = ReportStatus.entries.filter { it != ReportStatus.ARCHIEVED }.map { it.name },
                availableRiskLevels = listOf("high", "medium", "low"),
            ),
        )
    }

    fun findDetailById(id: Long, viewerUserId: Long, isModerator: Boolean) = reportRepository.findById(id)
        .orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        .let { report ->
            if (report.status == ReportStatus.ARCHIEVED) {
                throw NotFoundException("Отчёт с id=$id не найден")
            }
            if (!isModerator && report.userId != viewerUserId) {
                throw NotFoundException("Отчёт с id=$id не найден")
            }
            val user = report.userId?.let { userRepository.findById(it).orElse(null) }
            report.toReportDetailResponse(user)
        }

    fun findByVkId(vkId: String, page: Int, size: Int): ReportsListResponse {
        val normalizedVkId = vkId.trim()
        if (normalizedVkId.isBlank()) throw ValidationException("vkId не может быть пустым")
        val user = userRepository.findByVkId(normalizedVkId)
            ?: throw NotFoundException("Пользователь с vkId=$normalizedVkId не найден")

        return findList(
            page = page,
            size = size,
            status = null,
            riskLevel = null,
            regionId = null,
            scopeUserId = user.id,
        )
    }

    fun findDetailByIdForBot(id: Long): ReportDetailResponse {
        val report = reportRepository.findById(id)
            .orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        if (report.status == ReportStatus.ARCHIEVED) {
            throw NotFoundException("Отчёт с id=$id не найден")
        }
        val user = report.userId?.let { userRepository.findById(it).orElse(null) }
        return report.toReportDetailResponse(user)
    }

    fun findAllDetailsByVkIdForBot(vkId: String): BotReportsDetailListResponse {
        val normalizedVkId = vkId.trim()
        if (normalizedVkId.isBlank()) throw ValidationException("vkId не может быть пустым")
        val user = userRepository.findByVkId(normalizedVkId)
            ?: throw NotFoundException("Пользователь с vkId=$normalizedVkId не найден")
        val uid = user.id ?: throw NotFoundException("Пользователь с vkId=$normalizedVkId не найден")
        val rows = reportRepository.findAllNonArchivedByUserId(uid)
        val reportUser = userRepository.findById(uid).orElse(null)
        val details = rows.map { it.toReportDetailResponse(reportUser) }
        return BotReportsDetailListResponse(reports = details, total = details.size)
    }

    @Transactional
    fun confirm(id: Long, request: ConfirmReportRequest): ReportResponse {
        val comment = request.comment.trim()
        if (comment.isBlank()) throw ValidationException("comment не может быть пустым")
        val report = reportRepository.findById(id).orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        if (report.status == ReportStatus.ARCHIEVED) throw NotFoundException("Отчёт с id=$id не найден")
        report.comment = comment
        report.status = ReportStatus.CONFIRMED
        return reportRepository.save(report).toReportResponse()
    }

    @Transactional
    fun decline(id: Long): ReportResponse {
        val report = reportRepository.findById(id).orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        if (report.status == ReportStatus.ARCHIEVED) throw NotFoundException("Отчёт с id=$id не найден")
        report.comment = null
        report.status = ReportStatus.DECLINED
        return reportRepository.save(report).toReportResponse()
    }

    @Transactional
    fun archiveProcessedReports(): Int = reportRepository.archiveProcessedReports()
}
