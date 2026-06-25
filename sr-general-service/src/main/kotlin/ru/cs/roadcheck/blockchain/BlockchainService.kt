package ru.cs.roadcheck.blockchain

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.web3j.protocol.Web3j
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Numeric
import java.math.BigInteger

private val logger = KotlinLogging.logger {}

data class BlockchainRecordResult(
    val transactionHash: String,
    val blockNumber: Long,
)

@Service
class BlockchainService(
    private val properties: BlockchainProperties,
    @Autowired(required = false) private val rawTransactionManager: RawTransactionManager?,
    @Autowired(required = false) private val web3j: Web3j?,
) {

    fun isAvailable(): Boolean = properties.isConfigured() && rawTransactionManager != null && web3j != null

    fun recordReport(reportId: Long, reportContentHash: String): BlockchainRecordResult? {
        if (!isAvailable()) {
            logger.debug { "Blockchain skipped for report $reportId: not available (enabled=${properties.enabled}, rpc=${properties.rpcUrl.takeIf { it.isNotBlank() } != null})" }
            return null
        }
        return try {
            val data = encodeData("report", reportId, reportContentHash)
            val response = rawTransactionManager!!.sendTransaction(
                GAS_PRICE,
                GAS_LIMIT,
                ZERO_ADDRESS,
                data,
                BigInteger.ZERO,
            )
            if (response.hasError()) {
                logger.error { "Blockchain RPC error for report $reportId: ${response.error?.message}" }
                return null
            }
            val txHash = response.getTransactionHash()
            if (txHash.isNullOrBlank()) {
                logger.error { "Blockchain: no tx hash returned for report $reportId" }
                return null
            }
            val blockNumber = web3j?.let { waitForReceipt(it, txHash) } ?: 0L
            logger.info { "Report $reportId recorded on blockchain: tx=$txHash, block=$blockNumber" }
            BlockchainRecordResult(transactionHash = txHash, blockNumber = blockNumber)
        } catch (e: Exception) {
            logger.error(e) { "Blockchain record failed for report $reportId: ${e.message}" }
            null
        }
    }

    fun recordUser(userId: Long, userContentHash: String): BlockchainRecordResult? {
        if (!isAvailable()) return null
        return try {
            val data = encodeData("user", userId, userContentHash)
            val response = rawTransactionManager!!.sendTransaction(
                GAS_PRICE, GAS_LIMIT, ZERO_ADDRESS, data, BigInteger.ZERO,
            )
            if (response.hasError()) {
                logger.error { "Blockchain RPC error for user $userId: ${response.error?.message}" }
                return null
            }
            val txHash = response.getTransactionHash()
            if (txHash.isNullOrBlank()) return null
            val blockNumber = web3j?.let { waitForReceipt(it, txHash) } ?: 0L
            logger.info { "User $userId recorded on blockchain: tx=$txHash, block=$blockNumber" }
            BlockchainRecordResult(transactionHash = txHash, blockNumber = blockNumber)
        } catch (e: Exception) {
            logger.error(e) { "Blockchain record failed for user $userId: ${e.message}" }
            null
        }
    }

    private fun encodeData(type: String, id: Long, contentHash: String): String {
        val payload = "RoadCheck:$type:$id:$contentHash"
        return Numeric.toHexString(payload.toByteArray(Charsets.UTF_8))
    }

    private fun waitForReceipt(web3j: Web3j, txHash: String, maxAttempts: Int = 10): Long {
        for (i in 0 until maxAttempts) {
            val receipt = web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt
            if (receipt.isPresent) {
                val r = receipt.get()
                val blockNum = r.blockNumber?.toString()?.let { Numeric.toBigInt(it).toLong() } ?: 0L
                if (blockNum > 0) return blockNum
            }
            Thread.sleep(100L * (i + 1))
        }
        return 0L
    }
}

private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
private val GAS_PRICE = BigInteger.valueOf(1_000_000_000L)
private val GAS_LIMIT = BigInteger.valueOf(50_000L)
