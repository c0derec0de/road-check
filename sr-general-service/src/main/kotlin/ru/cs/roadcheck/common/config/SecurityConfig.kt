package ru.cs.roadcheck.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import ru.cs.roadcheck.auth.ApiTokenAuthFilter
import ru.cs.roadcheck.auth.JwtAuthFilter
import ru.cs.roadcheck.auth.NginxOnlyFilter
import ru.cs.roadcheck.config.BotProperties

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(BotProperties::class)
class SecurityConfig(
    private val nginxOnlyFilter: NginxOnlyFilter,
    private val jwtAuthFilter: JwtAuthFilter,
    private val apiTokenAuthFilter: ApiTokenAuthFilter,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/auth/**").permitAll()
                auth.requestMatchers("/actuator/health").permitAll()
                auth.requestMatchers("/api/regions/**").permitAll()
                auth.requestMatchers("/docs/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/predictions/charts/**").permitAll()
                auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                auth.requestMatchers("/api/internal/bot/**").hasRole("BOT")
                auth.requestMatchers("/api/manager/**").hasRole("MODERATOR")
                auth.requestMatchers(HttpMethod.PUT, "/api/reports/**").hasRole("MODERATOR")
                auth.requestMatchers("/api/predictions/**").hasRole("MODERATOR")
                auth.requestMatchers("/api/reports/**").hasAnyRole("USER", "MODERATOR")
                auth.requestMatchers("/api/dashboard/**").hasAnyRole("USER", "MODERATOR")
                auth.requestMatchers("/api/analytics/**").hasAnyRole("USER", "MODERATOR")
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(nginxOnlyFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(apiTokenAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthFilter, ApiTokenAuthFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
