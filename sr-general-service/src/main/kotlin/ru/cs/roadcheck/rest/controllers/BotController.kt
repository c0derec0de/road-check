package ru.cs.roadcheck.rest.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.cs.roadcheck.common.exception.ValidationException
import ru.cs.roadcheck.rest.dto.BotReportsDetailListResponse
import ru.cs.roadcheck.rest.dto.BotRegisterRequest
import ru.cs.roadcheck.rest.dto.BotReportRequest
import ru.cs.roadcheck.rest.dto.BotUserExistsResponse
import ru.cs.roadcheck.rest.dto.RegisterResponse
import ru.cs.roadcheck.rest.dto.ReportDetailResponse
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.ReportsListResponse
import ru.cs.roadcheck.config.BotProperties
import ru.cs.roadcheck.service.AuthService
import ru.cs.roadcheck.service.ReportService

@Tag(name = "Bot API (internal)", description = "Внутренние методы для интеграции с ботом")
@RestController
@RequestMapping("/api/internal/bot")
class BotController(
    private val authService: AuthService,
    private val reportService: ReportService,
    private val botProperties: BotProperties,
) {

    @Operation(
        summary = "Регистрация пользователя ботом",
        description = "Регистрация нового пользователя через бота с автоматической привязкой VK ID. Требует заголовок X-API-TOKEN.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован", content = [Content(schema = Schema(implementation = RegisterResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
            ApiResponse(responseCode = "401", description = "Неверный или отсутствующий X-API-TOKEN"),
            ApiResponse(responseCode = "409", description = "Пользователь с таким логином/email/телефоном/VK ID уже существует"),
        ],
    )
    @PostMapping("/register")
    fun register(
        @Parameter(description = "Сервисный API токен", required = true)
        @RequestHeader("X-API-TOKEN") apiToken: String,
        @RequestBody request: BotRegisterRequest,
    ): ResponseEntity<RegisterResponse> {
        validateApiToken(apiToken)
        val response = authService.registerByBot(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "Создание отчёта ботом",
        description = "Создание нового отчёта от имени пользователя по VK ID. Требует заголовок X-API-TOKEN.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Отчёт успешно создан", content = [Content(schema = Schema(implementation = ReportResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
            ApiResponse(responseCode = "401", description = "Неверный или отсутствующий X-API-TOKEN"),
            ApiResponse(responseCode = "404", description = "Пользователь с указанным VK ID не найден"),
        ],
    )
    @PostMapping("/reports")
    fun createReport(
        @Parameter(description = "Сервисный API токен", required = true)
        @RequestHeader("X-API-TOKEN") apiToken: String,
        @RequestBody request: BotReportRequest,
    ): ResponseEntity<ReportResponse> {
        validateApiToken(apiToken)
        val response = reportService.createByBot(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "Все отчёты пользователя (детальные карточки)",
        description = "Возвращает все неархивированные отчёты пользователя в формате ReportDetailResponse (от новых к старым). Для постраничного краткого списка используйте GET /api/internal/bot/reports. Требует заголовок X-API-TOKEN.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Список сформирован (может быть пустым)", content = [Content(schema = Schema(implementation = BotReportsDetailListResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            ApiResponse(responseCode = "401", description = "Неверный или отсутствующий X-API-TOKEN"),
            ApiResponse(responseCode = "404", description = "Пользователь с указанным VK ID не найден"),
        ],
    )
    @GetMapping("/reports/detailed-all")
    fun getAllReportsDetailed(
        @Parameter(description = "Сервисный API токен", required = true)
        @RequestHeader("X-API-TOKEN") apiToken: String,
        @Parameter(description = "VK ID пользователя")
        @RequestParam("vkId") vkId: String,
    ): ResponseEntity<BotReportsDetailListResponse> {
        validateApiToken(apiToken)
        val response = reportService.findAllDetailsByVkIdForBot(vkId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Получение отчётов пользователя по VK ID",
        description = "Получение списка отчётов пользователя по его VK ID. Требует заголовок X-API-TOKEN.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Отчёты успешно получены", content = [Content(schema = Schema(implementation = ReportsListResponse::class))]),
            ApiResponse(responseCode = "401", description = "Неверный или отсутствующий X-API-TOKEN"),
            ApiResponse(responseCode = "404", description = "Пользователь с указанным VK ID не найден"),
        ],
    )
    @GetMapping("/reports")
    fun getReports(
        @Parameter(description = "Сервисный API токен", required = true)
        @RequestHeader("X-API-TOKEN") apiToken: String,
        @Parameter(description = "VK ID пользователя")
        @RequestParam("vkId") vkId: String,
        @Parameter(description = "Номер страницы", example = "1")
        @RequestParam("page", defaultValue = "1") page: Int,
        @Parameter(description = "Размер страницы", example = "20")
        @RequestParam("size", defaultValue = "20") size: Int,
    ): ResponseEntity<ReportsListResponse> {
        validateApiToken(apiToken)
        val response = reportService.findByVkId(vkId, page, size)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Получение детальной информации об отчёте",
        description = "Получение полной информации об отчёте по его ID. Требует заголовок X-API-TOKEN.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Отчёт успешно получен", content = [Content(schema = Schema(implementation = ReportDetailResponse::class))]),
            ApiResponse(responseCode = "401", description = "Неверный или отсутствующий X-API-TOKEN"),
            ApiResponse(responseCode = "404", description = "Отчёт с указанным ID не найден"),
        ],
    )
    @GetMapping("/reports/{id}")
    fun getReportDetail(
        @Parameter(description = "Сервисный API токен", required = true)
        @RequestHeader("X-API-TOKEN") apiToken: String,
        @Parameter(description = "ID отчёта")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ReportDetailResponse> {
        validateApiToken(apiToken)
        val response = reportService.findDetailByIdForBot(id)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Проверка наличия пользователя по VK ID",
        description = "Возвращает, существует ли пользователь с указанным VK ID. Требует заголовок X-API-TOKEN.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Проверка выполнена", content = [Content(schema = Schema(implementation = BotUserExistsResponse::class))]),
            ApiResponse(responseCode = "401", description = "Неверный или отсутствующий X-API-TOKEN"),
        ],
    )
    @GetMapping("/users/exists")
    fun userExistsByVkId(
        @Parameter(description = "Сервисный API токен", required = true)
        @RequestHeader("X-API-TOKEN") apiToken: String,
        @Parameter(description = "VK ID пользователя")
        @RequestParam("vkId") vkId: String,
    ): ResponseEntity<BotUserExistsResponse> {
        validateApiToken(apiToken)
        return ResponseEntity.ok(authService.checkUserExistsByVkId(vkId))
    }

    private fun validateApiToken(apiToken: String) {
        if (apiToken.isBlank()) {
            throw ValidationException("X-API-TOKEN не может быть пустым")
        }
        if (apiToken != botProperties.apiToken) {
            throw ValidationException("Неверный X-API-TOKEN")
        }
    }
}
