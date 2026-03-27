package com.example.leadflow.shared

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.validation.BindException
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.slf4j.LoggerFactory

@RestControllerAdvice
class GlobalExceptionHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException): ResponseEntity<ApiError> =
        ResponseEntity.status(exception.status).body(ApiError(exception.status, exception.message ?: "Unexpected error"))

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class, WebExchangeBindException::class)
    fun handleValidationException(exception: Exception): ResponseEntity<ApiError> {
        val message =
            when (exception) {
                is MethodArgumentNotValidException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
                is BindException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
                is WebExchangeBindException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
                else -> null
            } ?: "Validation failed"

        return ResponseEntity.badRequest().body(ApiError(400, message))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ApiError> {
        logger.error("Unhandled exception in leadflow backend", exception)
        return ResponseEntity.internalServerError().body(ApiError(500, exception.message ?: exception.javaClass.simpleName))
    }
}
