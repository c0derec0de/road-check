package ru.cs.roadcheck.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secret: String = "roadcheck-default-secret-change-in-production-min-256-bits",
    var expirationMs: Long = 86400000,
    var issuer: String = "roadcheck",
)
