package ru.cs.roadcheck.rest.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.Authentication
import ru.cs.roadcheck.auth.isModerator
import ru.cs.roadcheck.auth.userId
import ru.cs.roadcheck.rest.dto.DangerousZonesListResponse
import ru.cs.roadcheck.service.AnalyticsService

@Tag(name = "Аналитика", description = "Аналитические данные")
@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService,
) {

    @Operation(
        summary = "Список опасных зон",
        description = "Возвращает список всех активных опасных зон с количеством инцидентов и уровнем риска. Можно отфильтровать по региону.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Список зон получен", content = [Content(schema = Schema(implementation = DangerousZonesListResponse::class))]),
        ],
    )
    @GetMapping("/dangerous-zones")
    fun getDangerousZones(
        auth: Authentication,
        @Parameter(description = "ID региона для фильтрации", example = "1")
        @RequestParam(required = false) regionId: Long?,
    ) = ResponseEntity.ok(
        if (auth.isModerator()) analyticsService.getDangerousZones(regionId)
        else analyticsService.getDangerousZonesForUser(auth.userId(), regionId),
    )
}
