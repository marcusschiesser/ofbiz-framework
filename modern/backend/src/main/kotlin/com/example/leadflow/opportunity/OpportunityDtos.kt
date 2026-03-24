package com.example.leadflow.opportunity

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal

enum class OpportunityStage {
    NEW,
    BRIEF_READY,
    QUOTE_READY,
}

data class OpportunityCreateRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:Email @field:NotBlank val email: String,
    val companyName: String? = null,
    val title: String? = null,
    val dataSourceId: String? = "WEB_SITE",
)

data class OpportunityLineInput(
    @field:NotBlank val description: String,
    val productId: String? = null,
    @field:DecimalMin("0.01") val quantity: BigDecimal,
    @field:DecimalMin("0.00") val unitPrice: BigDecimal,
)

data class OpportunityBriefInput(
    @field:NotBlank val title: String,
    val notes: String? = null,
    @field:Valid @field:NotEmpty val lines: List<OpportunityLineInput>,
)

data class OpportunityLineDetail(
    val description: String,
    val productId: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
)

data class OpportunityBriefDetail(
    val title: String,
    val notes: String?,
    val lines: List<OpportunityLineDetail>,
    val isLocked: Boolean,
)

data class OpportunityQuoteItemSummary(
    val seqId: String,
    val description: String,
    val productId: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
)

data class OpportunityQuoteSummary(
    val quoteId: String,
    val statusId: String,
    val total: BigDecimal,
    val items: List<OpportunityQuoteItemSummary>,
)

data class OpportunityBackOfficeLinks(
    val contactRecordPath: String,
    val accountRecordPath: String?,
    val quoteRecordPath: String?,
)

data class OpportunitySummary(
    val partyId: String,
    val displayName: String,
    val companyName: String?,
    val email: String?,
    val stage: OpportunityStage,
    val currentValue: BigDecimal,
    val nextAction: String,
    val latestQuoteId: String?,
)

data class OpportunityDetail(
    val partyId: String,
    val displayName: String,
    val companyPartyId: String?,
    val companyName: String?,
    val email: String?,
    val stage: OpportunityStage,
    val currentValue: BigDecimal,
    val nextAction: String,
    val latestQuoteId: String?,
    val brief: OpportunityBriefDetail?,
    val quote: OpportunityQuoteSummary?,
    val backOfficeLinks: OpportunityBackOfficeLinks,
)
