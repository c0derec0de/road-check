package ru.cs.roadcheck.rest.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.cs.roadcheck.auth.userId
import ru.cs.roadcheck.rest.dto.BindVkIdRequest
import ru.cs.roadcheck.rest.dto.LoginRequest
import ru.cs.roadcheck.rest.dto.LoginResponse
import ru.cs.roadcheck.rest.dto.RegisterRequest
import ru.cs.roadcheck.rest.dto.RegisterResponse
import ru.cs.roadcheck.service.AuthService

@Tag(name = "Авторизация", description = "Регистрация, вход и выход")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(summary = "Регистрация", description = "Создание пользователя с паролем (BCrypt), запись в блокчейн")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Пользователь создан", content = [Content(schema = Schema(implementation = RegisterResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ],
    )
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @Operation(
        summary = "Вход в систему",
        description = "Авторизация по login/email/vkId/phone и паролю. Возвращает JWT токен.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Успешный вход", content = [Content(schema = Schema(implementation = LoginResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            ApiResponse(responseCode = "401", description = "Неверный логин или пароль", content = [Content(schema = Schema(implementation = LoginResponse::class))]),
        ],
    )
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = authService.login(request)
        return ResponseEntity
            .status(if (response.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
            .body(response)
    }

    @Operation(
        summary = "Привязка VK ID",
        description = "Добавляет/обновляет VK ID для текущего пользователя.",
    )
    @PostMapping("/vk-id")
    fun bindVkId(auth: Authentication, @RequestBody request: BindVkIdRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(authService.bindVkId(auth.userId(), request))

    @Operation(
        summary = "Выход из системы",
        description = "Завершение сессии пользователя.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Выход выполнен", content = [Content(schema = Schema(implementation = LoginResponse::class))]),
        ],
    )
    @PostMapping("/logout")
    fun logout() = ResponseEntity.ok(authService.logout())
}
