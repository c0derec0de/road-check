package ru.cs.roadcheck.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.cs.roadcheck.config.BotProperties

@Component
class ApiTokenAuthFilter(
    private val botProperties: BotProperties,
) : OncePerRequestFilter() {

    companion object {
        const val API_TOKEN_HEADER = "X-API-TOKEN"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.servletPath.startsWith("/api/internal/bot")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val apiToken = request.getHeader(API_TOKEN_HEADER)?.trim()

        if (apiToken.isNullOrEmpty()) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.characterEncoding = Charsets.UTF_8.name()
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"message":"X-API-TOKEN header is required"}""")
            response.writer.flush()
            return
        }

        if (!isValidApiToken(apiToken)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.characterEncoding = Charsets.UTF_8.name()
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"message":"Invalid X-API-TOKEN"}""")
            response.writer.flush()
            return
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_BOT"))
        val authentication = UsernamePasswordAuthenticationToken(
            "bot-service",
            apiToken,
            authorities
        )
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }

    private fun isValidApiToken(token: String): Boolean {
        return token == botProperties.apiToken
    }
}
