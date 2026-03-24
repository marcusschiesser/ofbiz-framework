package com.example.leadflow.lead

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LeadCreateRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:Email @field:NotBlank val email: String,
    val companyName: String? = null,
    val title: String? = null,
    val dataSourceId: String? = "WEB_SITE",
)

data class LeadSummary(
    val partyId: String,
    val fullName: String,
    val companyName: String?,
    val statusId: String,
    val email: String?,
    val requestCount: Int,
)

data class LeadDetail(
    val partyId: String,
    val fullName: String,
    val companyPartyId: String?,
    val companyName: String?,
    val statusId: String,
    val email: String?,
    val requestCount: Int,
)
