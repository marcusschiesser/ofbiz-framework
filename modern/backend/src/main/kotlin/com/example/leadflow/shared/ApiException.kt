package com.example.leadflow.shared

open class ApiException(
    message: String,
) : RuntimeException(message)

class NotFoundException(
    message: String,
) : ApiException(message)

class ConflictException(
    message: String,
) : ApiException(message)
