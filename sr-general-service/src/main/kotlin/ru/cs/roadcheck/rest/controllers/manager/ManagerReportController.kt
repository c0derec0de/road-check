package ru.cs.roadcheck.rest.controllers.manager

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.cs.roadcheck.rest.dto.ReportResponse
import ru.cs.roadcheck.rest.dto.manager.ReportManagerRequest
import ru.cs.roadcheck.service.manager.ReportManagerService

@Tag(name = "Управление отчётами")
@RestController
@RequestMapping("/api/manager/reports")
class ManagerReportController(private val service: ReportManagerService) {

    @GetMapping
    @Operation(summary = "Список отчётов")
    fun list(): ResponseEntity<List<ReportResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Отчёт по id")
    fun getById(@PathVariable id: Long) = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Создать отчёт")
    fun create(@RequestBody request: ReportManagerRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить отчёт")
    fun update(@PathVariable id: Long, @RequestBody request: ReportManagerRequest) =
        ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить отчёт")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
