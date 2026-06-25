package ru.cs.roadcheck.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.PathItem.HttpMethod
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class OpenApiConfig(
    private val environment: Environment,
) {

    @Bean
    fun nginxHeaderAndAccessForSwaggerUi(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.paths?.forEach { (path, pathItem) ->
            pathItem.readOperationsMap().forEach { (method, operation) ->
                val hasHeader = operation.parameters?.any { p ->
                    p.name == "X-From-Nginx" && "header".equals(p.`in`, ignoreCase = true)
                } == true
                if (!hasHeader) {
                    operation.addParametersItem(
                        Parameter()
                            .name("X-From-Nginx")
                            .`in`("header")
                            .required(false)
                            .description(
                                "Прямой вызов к Spring: укажите `1`. За nginx этот заголовок выставляет сам прокси.",
                            )
                            .schema(
                                StringSchema().apply { setDefault("1") },
                            ),
                    )
                }
                applySecurityRequirements(path, method, operation)
                appendAccessDescription(path, method, operation)
            }
        }
    }

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("RoadCheck API")
                .description(
                    """
                    REST API для учёта отчётов и авторизации RoadCheck.

                    **Пользовательский API:** `Authorization: Bearer <JWT>` после `/api/auth/login` или `/api/auth/register`.

                    **Bot API:** только пути `/api/internal/bot/**` — заголовок `X-API-TOKEN` (секрет `BOT_API_TOKEN`).

                    **Проверка nginx-заголовка включена всегда:** каждый запрос к приложению должен содержать `X-From-Nginx: 1` (через nginx заголовок добавляет прокси автоматически).

                    **Доступы:**
                    - Публично: `/api/auth/**`, `/api/regions/**`, `GET /api/predictions/charts/**`
                    - Только JWT (`USER` или `MODERATOR`): `/api/reports/**`, `/api/dashboard/**`, `/api/analytics/**`
                    - Только `MODERATOR`: `/api/manager/**`, `PUT /api/reports/{id}/confirm`, `PUT /api/reports/{id}/decline`, `POST /api/predictions/run-manual`
                    """.trimIndent(),
                )
                .version("1.0")
                .contact(Contact().name("RoadCheck"))
                .license(License().name("Proprietary")),
        )
        .servers(
            listOf(
                Server()
                    .url(publicServerUrl())
                    .description("Nginx reverse proxy"),
                Server()
                    .url(directServerUrl())
                    .description("Прямой Spring Boot (для диагностики; нужен `X-From-Nginx: 1`)"),
            ),
        )
        .components(
            Components()
                .addSecuritySchemes(
                    "bearer-jwt",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT после успешной авторизации"),
                )
                .addSecuritySchemes(
                    "bot-api-token",
                    SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name("X-API-TOKEN")
                        .description("Секрет для внутренних ручек бота"),
                ),
        )

    private fun appendAccessDescription(path: String, method: HttpMethod, operation: io.swagger.v3.oas.models.Operation) {
        val access = when {
            path.startsWith("/api/internal/bot/") -> "Только BOT (`X-API-TOKEN`)"
            path.startsWith("/api/auth/") -> "Доступно всем (без JWT)"
            path.startsWith("/api/regions/") -> "Доступно всем (без JWT)"
            path.startsWith("/api/predictions/charts/") && method == HttpMethod.GET -> "Доступно всем (без JWT)"
            path.startsWith("/api/manager/") -> "Только MODERATOR"
            path.startsWith("/api/predictions/") -> "Только MODERATOR"
            path == "/api/reports/{id}/confirm" && method == HttpMethod.PUT -> "Только MODERATOR"
            path == "/api/reports/{id}/decline" && method == HttpMethod.PUT -> "Только MODERATOR"
            path.startsWith("/api/reports/") || path == "/api/reports" -> "JWT: USER или MODERATOR"
            path.startsWith("/api/dashboard/") -> "JWT: USER или MODERATOR"
            path.startsWith("/api/analytics/") -> "JWT: USER или MODERATOR"
            else -> "Требуется аутентификация"
        }

        val note = "\n\n**Доступ:** $access."
        val description = operation.description?.trim().orEmpty()
        if (!description.contains("**Доступ:**")) {
            operation.description = if (description.isBlank()) note.trim() else "$description$note"
        }
    }

    private fun applySecurityRequirements(path: String, method: HttpMethod, operation: io.swagger.v3.oas.models.Operation) {
        when {
            path.startsWith("/api/internal/bot/") ->
                operation.security = listOf(SecurityRequirement().addList("bot-api-token"))

            path.startsWith("/api/auth/") ||
                path.startsWith("/api/regions/") ||
                (path.startsWith("/api/predictions/charts/") && method == HttpMethod.GET) ||
                path.startsWith("/swagger-ui/") ||
                path == "/swagger-ui.html" ||
                path.startsWith("/v3/api-docs") ||
                path == "/actuator/health" ->
                operation.security = emptyList()

            path.startsWith("/api/manager/") ||
                path.startsWith("/api/predictions/") ||
                path.startsWith("/api/reports/") ||
                path == "/api/reports" ||
                path.startsWith("/api/dashboard/") ||
                path.startsWith("/api/analytics/") ->
                operation.security = listOf(SecurityRequirement().addList("bearer-jwt"))

            else -> operation.security = listOf(SecurityRequirement().addList("bearer-jwt"))
        }
    }

    private fun publicServerUrl(): String {
        val isProd = environment.activeProfiles.any { it.equals("prod", ignoreCase = true) }
        if (isProd) {
            val hostIp = System.getenv("HOST_IP")?.trim().orEmpty()
            if (hostIp.isNotBlank()) {
                return "http://$hostIp"
            }
        }
        return "http://localhost"
    }

    private fun directServerUrl(): String {
        val isProd = environment.activeProfiles.any { it.equals("prod", ignoreCase = true) }
        if (isProd) {
            val hostIp = System.getenv("HOST_IP")?.trim().orEmpty()
            if (hostIp.isNotBlank()) {
                return "http://$hostIp:8081"
            }
        }
        return "http://localhost:8081"
    }
}
