package ru.cs.roadcheck.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("bot")
data class BotProperties(
    val apiToken: String = "",
)
