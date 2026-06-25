package ru.cs.roadcheck.blockchain

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blockchain")
data class BlockchainProperties(
    var enabled: Boolean = false,
    var rpcUrl: String = "http://localhost:8545",
    var privateKey: String = "",
) {
    fun isConfigured(): Boolean = enabled && rpcUrl.isNotBlank() && privateKey.isNotBlank()
}
