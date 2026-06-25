package ru.cs.roadcheck.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import io.swagger.v3.oas.annotations.media.Schema
import ru.cs.roadcheck.common.exception.NotFoundException
import ru.cs.roadcheck.common.exception.ValidationException

@Schema(description = "Тело ответа при ошибке")
data class ErrorResponse(
    @Schema(description = "Сообщение об ошибке")
    val message: String,
    @Schema(description = "HTTP-код ответа")
    val status: Int,
)

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleBadRequest(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    message = "Некорректное тело запроса. Проверьте формат JSON и типы полей.",
                    status = HttpStatus.BAD_REQUEST.value(),
                ),
            )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(ex: MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = "${ex.headerName} не может быть пустым", status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Ошибка валидации", status = HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Не найдено", status = HttpStatus.NOT_FOUND.value()))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(message = "Недостаточно прав для выполнения операции", status = HttpStatus.FORBIDDEN.value()))
    }

    @ExceptionHandler(Throwable::class)
    fun handleOther(ex: Throwable): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Unhandled error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    message = "Внутренняя ошибка сервера. Повторите попытку позже или обратитесь к администратору.",
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ),
            )
    }
}
