package ru.cs.roadcheck.rest.controllers.manager

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.cs.roadcheck.rest.dto.manager.DangerousZoneManagerRequest
import ru.cs.roadcheck.rest.dto.manager.DangerousZoneManagerResponse
import ru.cs.roadcheck.service.manager.DangerousZoneManagerService

@Tag(name = "Управление опасными зонами")
@RestController
@RequestMapping("/api/manager/dangerous-zones")
class ManagerDangerousZoneController(private val service: DangerousZoneManagerService) {

    @GetMapping
    @Operation(summary = "Список опасных зон")
    fun list(): ResponseEntity<List<DangerousZoneManagerResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Опасная зона по id")
    fun getById(@PathVariable id: Long) = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Создать опасную зону")
    fun create(@RequestBody request: DangerousZoneManagerRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить опасную зону")
    fun update(@PathVariable id: Long, @RequestBody request: DangerousZoneManagerRequest) =
        ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить опасную зону")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
