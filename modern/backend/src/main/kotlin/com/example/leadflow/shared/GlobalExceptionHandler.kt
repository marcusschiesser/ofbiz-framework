package com.example.leadflow.shared

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException, request: HttpServletRequest): ResponseEntity<ApiError> =
        response(HttpStatus.NOT_FOUND, exception.message ?: "Not found", request)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(exception: ConflictException, request: HttpServletRequest): ResponseEntity<ApiError> =
        response(HttpStatus.CONFLICT, exception.message ?: "Conflict", request)

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        BindException::class,
        ConstraintViolationException::class,
        IllegalArgumentException::class,
    )
    fun handleBadRequest(exception: Exception, request: HttpServletRequest): ResponseEntity<ApiError> =
        response(HttpStatus.BAD_REQUEST, exception.message ?: "Bad request", request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ApiError> =
        response(HttpStatus.INTERNAL_SERVER_ERROR, exception.message ?: "Unexpected error", request)

    private fun response(status: HttpStatus, message: String, request: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(
                status = status.value(),
                message = message,
                path = request.requestURI,
            )
        )
}
