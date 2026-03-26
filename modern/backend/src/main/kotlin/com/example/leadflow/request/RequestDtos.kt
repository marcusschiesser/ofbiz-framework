package com.example.leadflow.request

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal

data class OpportunityRequestLineInput(
    @field:NotBlank val description: String,
    val productId: String? = null,
    @field:DecimalMin("0.01") val quantity: BigDecimal,
    @field:DecimalMin("0.00") val unitPrice: BigDecimal,
    val story: String? = null,
)

data class OpportunityRequestInput(
    @field:NotBlank val name: String,
    val description: String? = null,
    val story: String? = null,
    @field:Valid @field:NotEmpty val lines: List<OpportunityRequestLineInput>,
)

data class OpportunityRequestLineDetail(
    val seqId: String,
    val description: String,
    val productId: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val story: String?,
    val statusId: String,
)

data class OpportunityRequestDetail(
    val requestId: String,
    val name: String,
    val description: String?,
    val story: String?,
    val lines: List<OpportunityRequestLineDetail>,
    val isLocked: Boolean,
)
