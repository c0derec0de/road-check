package ru.cs.roadcheck.rest.controllers.manager

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.cs.roadcheck.rest.dto.manager.RoadManagerRequest
import ru.cs.roadcheck.rest.dto.manager.RoadManagerResponse
import ru.cs.roadcheck.service.manager.RoadManagerService

@Tag(name = "Управление дорогами")
@RestController
@RequestMapping("/api/manager/roads")
class ManagerRoadController(private val service: RoadManagerService) {

    @GetMapping
    @Operation(summary = "Список дорог")
    fun list(): ResponseEntity<List<RoadManagerResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Дорога по id")
    fun getById(@PathVariable id: Long) = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Создать дорогу")
    fun create(@RequestBody request: RoadManagerRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить дорогу")
    fun update(@PathVariable id: Long, @RequestBody request: RoadManagerRequest) =
        ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить дорогу")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
