package ru.cs.roadcheck.rest.controllers.manager

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.cs.roadcheck.rest.dto.manager.UserManagerRequest
import ru.cs.roadcheck.rest.dto.manager.UserManagerResponse
import ru.cs.roadcheck.service.manager.UserManagerService

@Tag(name = "Управление пользователями")
@RestController
@RequestMapping("/api/manager/users")
class ManagerUserController(private val service: UserManagerService) {

    @GetMapping
    @Operation(summary = "Список пользователей")
    fun list(): ResponseEntity<List<UserManagerResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Пользователь по id")
    fun getById(@PathVariable id: Long) = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Создать пользователя")
    fun create(@RequestBody request: UserManagerRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    fun update(@PathVariable id: Long, @RequestBody request: UserManagerRequest) =
        ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
