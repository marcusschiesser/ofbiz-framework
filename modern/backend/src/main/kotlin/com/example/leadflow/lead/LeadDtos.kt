package com.example.leadflow.lead

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class OpportunityCreateRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:Email @field:NotBlank val email: String,
    val companyName: String? = null,
    val title: String? = null,
    val dataSourceId: String? = null,
)

data class LeadRecord(
    val partyId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val companyPartyId: String?,
    val companyName: String?,
)
