package com.example.leadflow.shared

open class ApiException(
    message: String,
    val status: Int,
) : RuntimeException(message)

class NotFoundException(message: String) : ApiException(message, 404)

class ConflictException(message: String) : ApiException(message, 409)

class BadRequestException(message: String) : ApiException(message, 400)
