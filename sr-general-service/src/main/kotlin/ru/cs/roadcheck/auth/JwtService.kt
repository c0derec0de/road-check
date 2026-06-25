package ru.cs.roadcheck.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import ru.cs.roadcheck.common.config.JwtProperties
import ru.cs.roadcheck.common.domain.entities.UserRole
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(private val properties: JwtProperties) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(properties.secret.toByteArray(Charsets.UTF_8))
    }

    fun generateToken(userId: Long, login: String, role: UserRole): String = Jwts.builder()
        .subject(userId.toString())
        .claim("login", login)
        .claim("role", role.name)
        .issuer(properties.issuer)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + properties.expirationMs))
        .signWith(key)
        .compact()

    fun parseToken(token: String): Claims? = try {
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
    } catch (e: ExpiredJwtException) { null } catch (e: Exception) { null }

    fun getUserId(token: String): Long? = parseToken(token)?.subject?.toLongOrNull()

    /** Токены без claim `role` обрабатываются как USER (см. JwtAuthFilter). */
    fun getRole(token: String): UserRole? = parseToken(token)?.get("role", String::class.java)
        ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
}
