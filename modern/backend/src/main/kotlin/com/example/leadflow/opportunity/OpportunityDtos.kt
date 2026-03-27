package com.example.leadflow.opportunity

import com.example.leadflow.lead.OpportunityCreateRequest
import com.example.leadflow.request.OpportunityRequestDetail
import com.example.leadflow.request.OpportunityRequestInput
import java.time.Instant

enum class OpportunityStage {
    NEW,
    REQUEST_READY,
    QUOTE_READY,
}

data class OpportunityListItem(
    val partyId: String,
    val displayName: String,
    val email: String,
    val companyName: String?,
    val stage: OpportunityStage,
    val nextAction: String,
    val hasRequest: Boolean,
    val createdAt: Instant?,
)

data class OpportunityListResponse(
    val items: List<OpportunityListItem>,
)

data class OpportunityDetail(
    val partyId: String,
    val displayName: String,
    val email: String,
    val companyPartyId: String?,
    val companyName: String?,
    val stage: OpportunityStage,
    val nextAction: String,
    val request: OpportunityRequestDetail?,
)

typealias CreateOpportunityRequest = OpportunityCreateRequest
typealias SaveOpportunityRequestRequest = OpportunityRequestInput
