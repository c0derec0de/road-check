package ru.cs.roadcheck.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.cs.roadcheck.common.config.JwtProperties
import ru.cs.roadcheck.common.domain.entities.UserRole
import java.util.Date
import javax.crypto.SecretKey

class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private lateinit var properties: JwtProperties
    private lateinit var key: SecretKey

    @BeforeEach
    fun setUp() {
        properties = JwtProperties(
            secret = "test-secret-key-min-256-bits-for-hs256-algorithm",
            expirationMs = 3600000L,
            issuer = "test-issuer"
        )
        key = Keys.hmacShaKeyFor(properties.secret.toByteArray(Charsets.UTF_8))
        jwtService = JwtService(properties)
    }

    @Test
    fun `generateToken creates valid JWT token`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.USER)

        assertThat(token).isNotNull()
        assertThat(token).isNotBlank()
    }

    @Test
    fun `generateToken contains correct subject`() {
        val token = jwtService.generateToken(42L, "testuser", UserRole.USER)

        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(claims.subject).isEqualTo("42")
    }

    @Test
    fun `generateToken contains correct login claim`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.USER)

        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(claims.get("login", String::class.java)).isEqualTo("testuser")
    }

    @Test
    fun `generateToken contains correct role claim`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.MODERATOR)

        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(claims.get("role", String::class.java)).isEqualTo("MODERATOR")
    }

    @Test
    fun `generateToken contains correct issuer`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.USER)

        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(claims.issuer).isEqualTo("test-issuer")
    }

    @Test
    fun `generateToken contains expiration time`() {
        val beforeToken = System.currentTimeMillis()
        val token = jwtService.generateToken(1L, "testuser", UserRole.USER)
        val afterToken = System.currentTimeMillis()

        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        val expiration = claims.expiration.time
        assertThat(expiration).isGreaterThan(beforeToken)
        assertThat(expiration).isLessThan(afterToken + 3600000L + 1000)
    }

    @Test
    fun `getUserId extracts user id from token`() {
        val token = jwtService.generateToken(123L, "testuser", UserRole.USER)

        val userId = jwtService.getUserId(token)

        assertThat(userId).isEqualTo(123L)
    }

    @Test
    fun `getUserId returns null for invalid token`() {
        val userId = jwtService.getUserId("invalid-token")

        assertThat(userId).isNull()
    }

    @Test
    fun `getUserId returns null for empty token`() {
        val userId = jwtService.getUserId("")

        assertThat(userId).isNull()
    }

    @Test
    fun `getRole extracts USER role from token`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.USER)

        val role = jwtService.getRole(token)

        assertThat(role).isEqualTo(UserRole.USER)
    }

    @Test
    fun `getRole extracts MODERATOR role from token`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.MODERATOR)

        val role = jwtService.getRole(token)

        assertThat(role).isEqualTo(UserRole.MODERATOR)
    }

    @Test
    fun `getRole returns null for token without role claim`() {
        val tokenWithoutRole = Jwts.builder()
            .subject("1")
            .claim("login", "testuser")
            .issuer(properties.issuer)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000L))
            .signWith(key)
            .compact()

        val role = jwtService.getRole(tokenWithoutRole)

        assertThat(role).isNull()
    }

    @Test
    fun `getRole returns null for invalid token`() {
        val role = jwtService.getRole("invalid-token")

        assertThat(role).isNull()
    }

    @Test
    fun `parseToken returns claims for valid token`() {
        val token = jwtService.generateToken(1L, "testuser", UserRole.USER)

        val claims = jwtService.parseToken(token)

        assertThat(claims).isNotNull()
        assertThat(claims?.subject).isEqualTo("1")
    }

    @Test
    fun `parseToken returns null for expired token`() {
        val expiredToken = Jwts.builder()
            .subject("1")
            .claim("login", "testuser")
            .issuer(properties.issuer)
            .issuedAt(Date(System.currentTimeMillis() - 10000))
            .expiration(Date(System.currentTimeMillis() - 5000))
            .signWith(key)
            .compact()

        val claims = jwtService.parseToken(expiredToken)

        assertThat(claims).isNull()
    }

    @Test
    fun `parseToken returns null for invalid token`() {
        val claims = jwtService.parseToken("invalid.token.here")

        assertThat(claims).isNull()
    }

    @Test
    fun `parseToken returns null for empty token`() {
        val claims = jwtService.parseToken("")

        assertThat(claims).isNull()
    }

    @Test
    fun `tokens with different users are different`() {
        val token1 = jwtService.generateToken(1L, "user1", UserRole.USER)
        val token2 = jwtService.generateToken(2L, "user2", UserRole.USER)

        assertThat(token1).isNotEqualTo(token2)
    }

    @Test
    fun `tokens with different roles are different`() {
        val tokenUser = jwtService.generateToken(1L, "testuser", UserRole.USER)
        val tokenModerator = jwtService.generateToken(1L, "testuser", UserRole.MODERATOR)

        assertThat(tokenUser).isNotEqualTo(tokenModerator)
    }
}
