package ru.cs.roadcheck.service.manager

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.blockchain.BlockchainService
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.manager.ReportManagerRequest
import ru.cs.roadcheck.rest.dto.manager.toReportResponse
import ru.cs.roadcheck.rest.dto.manager.toReport
import java.security.MessageDigest

@Service
class ReportManagerService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val blockchainService: BlockchainService,
) {

    fun findAll() = reportRepository.findAllByStatusNot(ReportStatus.ARCHIEVED).map { it.toReportResponse() }

    fun findById(id: Long) = reportRepository.findById(id)
        .orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        .let {
            if (it.status == ReportStatus.ARCHIEVED) throw NotFoundException("Отчёт с id=$id не найден")
            it.toReportResponse()
        }

    @Transactional
    fun create(request: ReportManagerRequest) = run {
        if (request.incidentType.isBlank()) throw ValidationException("incident_type не может быть пустым")
        if (!userRepository.existsById(request.policeUserId)) throw ValidationException("police_user_id не найден")
        if (!userRepository.existsById(request.userId)) throw ValidationException("user_id не найден")

        var saved = reportRepository.save(request.toReport())
        if (blockchainService.isAvailable()) {
            val contentHash = sha256("${saved.id}|${saved.incidentType}|${saved.latitude}|${saved.longitude}|${saved.description}|${saved.createdAt}")
            blockchainService.recordReport(saved.id!!, contentHash)?.let { result ->
                saved.blockchainTxHash = result.transactionHash
                saved.blockchainVerified = true
                saved.blockchainBlockNumber = result.blockNumber
                saved = reportRepository.save(saved)
            }
        }
        saved.toReportResponse()
    }

    @Transactional
    fun update(id: Long, request: ReportManagerRequest) = reportRepository.findById(id)
        .orElseThrow { NotFoundException("Отчёт с id=$id не найден") }
        .apply {
            if (request.incidentType.isNotBlank()) incidentType = request.incidentType
            if (!userRepository.existsById(request.policeUserId)) throw ValidationException("police_user_id не найден")
            if (!userRepository.existsById(request.userId)) throw ValidationException("user_id не найден")
            policeUserId = request.policeUserId
            userId = request.userId
            latitude = request.latitude
            longitude = request.longitude
            description = request.description
            photosUuid = request.photosUuid
            request.status?.trim()?.uppercase()?.let { status = ReportStatus.valueOf(it) }
            fatalities = request.fatalities
            injuries = request.injuries
            cause = request.cause
            riskLevel = request.riskLevel
            title = request.title
            address = request.address
            request.isDangerousZone?.let { isDangerousZone = it }
        }
        .let { reportRepository.save(it).toReportResponse() }

    @Transactional
    fun delete(id: Long) {
        if (!reportRepository.existsById(id)) throw NotFoundException("Отчёт с id=$id не найден")
        reportRepository.deleteById(id)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
