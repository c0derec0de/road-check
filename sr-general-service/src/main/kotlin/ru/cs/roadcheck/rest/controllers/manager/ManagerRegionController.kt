package ru.cs.roadcheck.rest.controllers.manager

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.cs.roadcheck.rest.dto.manager.RegionManagerRequest
import ru.cs.roadcheck.rest.dto.manager.RegionManagerResponse
import ru.cs.roadcheck.service.manager.RegionManagerService

@Tag(name = "Управление регионами")
@RestController
@RequestMapping("/api/manager/regions")
class ManagerRegionController(private val service: RegionManagerService) {

    @GetMapping
    @Operation(summary = "Список регионов")
    fun list(): ResponseEntity<List<RegionManagerResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Регион по id")
    fun getById(@PathVariable id: Long) = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Создать регион")
    fun create(@RequestBody request: RegionManagerRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить регион")
    fun update(@PathVariable id: Long, @RequestBody request: RegionManagerRequest) =
        ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить регион")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
