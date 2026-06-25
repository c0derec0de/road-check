package ru.cs.roadcheck.service.manager

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.cs.roadcheck.blockchain.BlockchainService
import ru.cs.roadcheck.common.domain.entities.ReportStatus
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.repository.ReportRepository
import ru.cs.roadcheck.repository.UserRepository
import ru.cs.roadcheck.rest.dto.manager.UserManagerRequest
import ru.cs.roadcheck.rest.dto.manager.toUserManagerResponse
import ru.cs.roadcheck.rest.dto.manager.toUser
import java.security.MessageDigest
import ru.cs.roadcheck.common.domain.entities.User

@Service
class UserManagerService(
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
    private val blockchainService: BlockchainService,
) {

    fun findAll() = userRepository.findAll().map { it.toUserManagerResponse() }

    fun findById(id: Long) = userRepository.findById(id)
        .orElseThrow { NotFoundException("Пользователь с id=$id не найден") }
        .toUserManagerResponse()

    @Transactional
    fun create(request: UserManagerRequest) = request.toUser().let { user ->
        var saved = userRepository.save(user)
        if (blockchainService.isAvailable()) {
            val contentHash = buildUserContentHash(saved)
            blockchainService.recordUser(saved.id!!, contentHash)?.let { result ->
                saved.blockchainTxHash = result.transactionHash
                saved.blockchainVerified = true
                saved = userRepository.save(saved)
            }
        }
        saved.toUserManagerResponse()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildUserContentHash(user: User): String =
        sha256("${user.id}|${user.login}|${user.email}|${user.phone}|${user.vkId}|${user.walletAddress}|${user.createdAt}")

    @Transactional
    fun update(id: Long, request: UserManagerRequest) = userRepository.findById(id)
        .orElseThrow { NotFoundException("Пользователь с id=$id не найден") }
        .apply {
            login = request.login
            vkId = request.vkId
            firstname = request.firstname
            middlename = request.middlename
            lastname = request.lastname
            department = request.department
            city = request.city
            phone = request.phone
            email = request.email
            walletAddress = request.walletAddress
        }
        .let { userRepository.save(it).toUserManagerResponse() }

    @Transactional
    fun delete(id: Long) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("Пользователь с id=$id не найден")
        }

        val reports = reportRepository.findAllByUserId(id)
        reports.forEach { report ->
            val previousStatus = report.status?.name ?: "UNKNOWN"
            report.userId = null
            report.status = ReportStatus.ARCHIEVED
            report.comment = buildArchiveComment(report.comment, id, previousStatus)

            if (blockchainService.isAvailable() && report.id != null) {
                val contentHash = buildArchivedReportContentHash(report.id!!, id, previousStatus)
                blockchainService.recordReport(report.id!!, contentHash)?.let { result ->
                    report.blockchainTxHash = result.transactionHash
                    report.blockchainVerified = true
                    report.blockchainBlockNumber = result.blockNumber
                }
            }
        }
        if (reports.isNotEmpty()) {
            reportRepository.saveAll(reports)
        }

        userRepository.deleteById(id)
    }

    private fun buildArchiveComment(currentComment: String?, deletedUserId: Long, previousStatus: String): String {
        val archiveNote = "Archived after user deletion: userId=$deletedUserId, previousStatus=$previousStatus"
        val normalizedComment = currentComment?.trim().orEmpty()
        return if (normalizedComment.isBlank()) archiveNote else "$normalizedComment | $archiveNote"
    }

    private fun buildArchivedReportContentHash(reportId: Long, deletedUserId: Long, previousStatus: String): String =
        sha256("report-archived-anonymized|$reportId|$deletedUserId|$previousStatus")
}
