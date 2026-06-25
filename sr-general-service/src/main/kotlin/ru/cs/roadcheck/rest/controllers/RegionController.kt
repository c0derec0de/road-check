package ru.cs.roadcheck.rest.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.cs.roadcheck.rest.dto.RegionResponse
import ru.cs.roadcheck.rest.dto.RegionWeatherResponse
import ru.cs.roadcheck.service.RegionService
import ru.cs.roadcheck.service.RegionWeatherService

@Tag(name = "Регионы", description = "Справочник регионов")
@RestController
@RequestMapping("/api/regions")
class RegionController(
    private val regionService: RegionService,
    private val regionWeatherService: RegionWeatherService,
) {

    @Operation(
        summary = "Список регионов",
        description = "Возвращает список всех регионов с координатами центра.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список регионов получен",
                content = [Content(schema = Schema(implementation = RegionResponse::class))],
            ),
        ],
    )
    @GetMapping
    fun list(): ResponseEntity<List<RegionResponse>> = ResponseEntity.ok(regionService.findAll())

    @Operation(
        summary = "Регион по id",
        description = "Возвращает регион по идентификатору с координатами центра.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Регион найден",
                content = [Content(schema = Schema(implementation = RegionResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "Регион не найден"),
        ],
    )
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<RegionResponse> =
        ResponseEntity.ok(regionService.findById(id))

    @Operation(
        summary = "Погода в регионе по id",
        description = "Ходит во внешнее API погоды по координатам региона и возвращает температуру и осадки.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Погода получена",
                content = [Content(schema = Schema(implementation = RegionWeatherResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "Регион не найден или нет данных погоды"),
        ],
    )
    @GetMapping("/{id}/weather")
    fun getWeather(@PathVariable id: Long): ResponseEntity<RegionWeatherResponse> =
        ResponseEntity.ok(regionWeatherService.getNow(id))
}

