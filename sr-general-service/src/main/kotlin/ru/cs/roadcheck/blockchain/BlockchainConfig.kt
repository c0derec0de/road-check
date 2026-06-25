package ru.cs.roadcheck.blockchain

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager

private val logger = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(BlockchainProperties::class)
class BlockchainConfig {

    @Bean
    @ConditionalOnProperty(name = ["blockchain.enabled"], havingValue = "true")
    fun web3j(properties: BlockchainProperties) = Web3j.build(HttpService(properties.rpcUrl))

    @Bean
    @ConditionalOnProperty(name = ["blockchain.enabled"], havingValue = "true")
    fun blockchainCredentials(properties: BlockchainProperties): Credentials {
        val raw = properties.privateKey.trim()
        if (raw.length < 64) throw IllegalStateException("blockchain.private-key must be 64 hex chars (with or without 0x)")
        val key = if (raw.startsWith("0x")) raw else "0x$raw"
        val creds = Credentials.create(key)
        logger.info { "Blockchain enabled: ${properties.rpcUrl}, sender=${creds.address}" }
        return creds
    }

    @Bean
    @ConditionalOnProperty(name = ["blockchain.enabled"], havingValue = "true")
    fun rawTransactionManager(web3j: Web3j, credentials: Credentials): RawTransactionManager {
        return RawTransactionManager(web3j, credentials, GANACHE_CHAIN_ID)
    }
}

private const val GANACHE_CHAIN_ID = 1337L
