package ru.cs.roadcheck.auth

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import ru.cs.roadcheck.config.BotProperties

class ApiTokenAuthFilterTest {

    private val botProperties: BotProperties = mockk()
    private val filter = ApiTokenAuthFilter(botProperties)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `does not set authentication for non-bot paths even with valid token`() {
        every { botProperties.apiToken } returns "secret-bot"
        SecurityContextHolder.clearContext()

        val request = MockHttpServletRequest()
        request.servletPath = "/api/reports"
        request.addHeader("X-API-TOKEN", "secret-bot")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> }

        filter.doFilter(request, response, chain)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `sets ROLE_BOT for bot path with valid token`() {
        every { botProperties.apiToken } returns "secret-bot"
        SecurityContextHolder.clearContext()

        val request = MockHttpServletRequest()
        request.servletPath = "/api/internal/bot/reports"
        request.addHeader("X-API-TOKEN", "secret-bot")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> }

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isNotNull
        assertThat(auth!!.authorities.map { it.authority }).contains("ROLE_BOT")
    }
}
