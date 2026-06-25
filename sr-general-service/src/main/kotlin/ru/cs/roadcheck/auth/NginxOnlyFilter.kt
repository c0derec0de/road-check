package ru.cs.roadcheck.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class NginxOnlyFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val fromNginx = request.getHeader(NGINX_HEADER_NAME)?.trim()
        if (fromNginx != NGINX_HEADER_VALUE) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.characterEncoding = Charsets.UTF_8.name()
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"message":"Direct access is forbidden. Use the reverse proxy or send header X-From-Nginx: 1."}""")
            response.writer.flush()
            return
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val NGINX_HEADER_NAME = "X-From-Nginx"
        const val NGINX_HEADER_VALUE = "1"
    }
}
