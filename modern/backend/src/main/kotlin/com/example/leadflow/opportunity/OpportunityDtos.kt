package com.example.leadflow.opportunity

import com.example.leadflow.lead.OpportunityCreateRequest
import com.example.leadflow.request.OpportunityRequestDetail
import com.example.leadflow.request.OpportunityRequestInput

enum class OpportunityStage {
    NEW,
    REQUEST_READY,
    QUOTE_READY,
}

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
