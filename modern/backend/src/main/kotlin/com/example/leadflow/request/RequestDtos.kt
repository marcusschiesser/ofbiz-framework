package com.example.leadflow.request

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal

data class RequestLineInput(
    @field:NotBlank val description: String,
    val productId: String? = null,
    @field:DecimalMin("0.01") val quantity: BigDecimal,
    @field:DecimalMin("0.00") val unitPrice: BigDecimal,
)

data class RequestCreateRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    @field:Valid @field:NotEmpty val lines: List<RequestLineInput>,
)

data class RequestItemDetail(
    val seqId: String,
    val description: String,
    val productId: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val statusId: String,
)

data class RequestSummary(
    val custRequestId: String,
    val name: String,
    val statusId: String,
    val lineCount: Int,
)

data class RequestDetail(
    val custRequestId: String,
    val name: String,
    val description: String?,
    val statusId: String,
    val leadPartyId: String,
    val items: List<RequestItemDetail>,
    val quoteIds: List<String>,
)
