package org.apache.ofbiz.migration.orderlifecycle

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun handleValidation(ex: Exception, request: HttpServletRequest): ProblemDetail {
        val problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problem.title = "Validation failed"
        problem.detail = when (ex) {
            is MethodArgumentNotValidException -> ex.bindingResult.fieldErrors.joinToString("; ") {
                "${it.field}: ${it.defaultMessage}"
            }

            is BindException -> ex.bindingResult.fieldErrors.joinToString("; ") {
                "${it.field}: ${it.defaultMessage}"
            }

            else -> "Validation failed"
        }
        problem.setProperty("path", request.requestURI)
        return problem
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ProblemDetail {
        val problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problem.title = "Constraint violation"
        problem.detail = ex.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" }
        problem.setProperty("path", request.requestURI)
        return problem
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException, request: HttpServletRequest): ProblemDetail {
        val problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problem.title = "Malformed request body"
        problem.detail = ex.mostSpecificCause.message ?: "Request body could not be read"
        problem.setProperty("path", request.requestURI)
        return problem
    }

    @ExceptionHandler(OrderLifecycleException::class)
    fun handleDomain(ex: OrderLifecycleException, request: HttpServletRequest): ProblemDetail {
        val problem = ProblemDetail.forStatus(ex.status)
        problem.title = ex.title
        problem.detail = ex.message
        problem.setProperty("path", request.requestURI)
        return problem
    }

    @ExceptionHandler(ErrorResponseException::class)
    fun handleErrorResponse(ex: ErrorResponseException): ProblemDetail = ex.body
}
