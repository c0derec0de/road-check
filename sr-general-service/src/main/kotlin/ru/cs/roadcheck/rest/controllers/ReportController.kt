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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.Authentication
import ru.cs.roadcheck.auth.isModerator
import ru.cs.roadcheck.auth.userId
import ru.cs.roadcheck.rest.dto.ConfirmReportRequest
import ru.cs.roadcheck.rest.dto.CreateReportRequest
import ru.cs.roadcheck.rest.dto.ReportDetailResponse
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.ReportsListResponse
import ru.cs.roadcheck.service.ReportService

@Tag(name = "Отчёты", description = "Создание и управление отчётами об инцидентах")
@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService,
) {

    @Operation(
        summary = "Создать отчёт",
        description = "Создаёт новый отчёт об инциденте и сохраняет его в БД. Статус по умолчанию — NEW.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Отчёт создан", content = [Content(schema = Schema(implementation = ReportResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации (обязательные поля не заполнены или пустые)"),
        ],
    )
    @PostMapping
    fun create(auth: Authentication, @RequestBody request: CreateReportRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(
            reportService.create(request, auth.userId(), auth.isModerator()),
        )

    @Operation(
        summary = "Список отчётов",
        description = "Возвращает список отчётов с пагинацией и фильтрами по статусу, уровню риска и региону.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Список отчётов с пагинацией", content = [Content(schema = Schema(implementation = ReportsListResponse::class))]),
        ],
    )
    @GetMapping
    fun list(
        auth: Authentication,
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "Количество элементов на странице", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Фильтр по статусу (NEW, IN_PROGRESS, CONFIRMED, DECLINED)", example = "IN_PROGRESS")
        @RequestParam(required = false) status: String?,
        @Parameter(description = "Фильтр по уровню риска (high, medium, low)", example = "medium")
        @RequestParam(required = false) riskLevel: String?,
        @Parameter(description = "Фильтр по региону", example = "1")
        @RequestParam(required = false) regionId: Long?,
    ): ResponseEntity<ReportsListResponse> {
        val scopeUserId = if (auth.isModerator()) null else auth.userId()
        return ResponseEntity.ok(reportService.findList(page, size, status, riskLevel, regionId, scopeUserId))
    }

    @Operation(
        summary = "Детали отчёта",
        description = "Возвращает детальную информацию об отчёте, включая данные пользователя, фотографии, местоположение и комментарии.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Отчёт найден", content = [Content(schema = Schema(implementation = ReportDetailResponse::class))]),
            ApiResponse(responseCode = "404", description = "Отчёт с указанным id не найден"),
        ],
    )
    @GetMapping("/{id}")
    fun getById(
        auth: Authentication,
        @Parameter(description = "Идентификатор отчёта", required = true, example = "1")
        @PathVariable id: Long,
    ) = ResponseEntity.ok(reportService.findDetailById(id, auth.userId(), auth.isModerator()))

    @Operation(
        summary = "Подтвердить отчёт",
        description = "Подтверждает отчёт, устанавливает статус CONFIRMED и сохраняет обязательный комментарий модератора.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Отчёт подтверждён", content = [Content(schema = Schema(implementation = ReportResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации (пустой comment)"),
            ApiResponse(responseCode = "404", description = "Отчёт с указанным id не найден"),
        ],
    )
    @PutMapping("/{id}/confirm")
    fun confirm(
        @Parameter(description = "Идентификатор отчёта", required = true, example = "1")
        @PathVariable id: Long,
        @RequestBody request: ConfirmReportRequest,
    ) = ResponseEntity.ok(reportService.confirm(id, request))

    @Operation(
        summary = "Отклонить отчёт",
        description = "Отклоняет отчёт и устанавливает статус DECLINED без комментария.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Отчёт отклонён", content = [Content(schema = Schema(implementation = ReportResponse::class))]),
            ApiResponse(responseCode = "404", description = "Отчёт с указанным id не найден"),
        ],
    )
    @PutMapping("/{id}/decline")
    fun decline(
        @Parameter(description = "Идентификатор отчёта", required = true, example = "1")
        @PathVariable id: Long,
    ) = ResponseEntity.ok(reportService.decline(id))
}
