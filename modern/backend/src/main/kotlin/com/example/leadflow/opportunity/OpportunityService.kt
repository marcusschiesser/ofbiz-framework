package com.example.leadflow.opportunity

import com.example.leadflow.lead.LeadCommandService
import com.example.leadflow.lead.OpportunityCreateRequest
import com.example.leadflow.request.OpportunityRequestInput
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpportunityService(
    private val leadCommandService: LeadCommandService,
    private val requestCommandService: com.example.leadflow.request.RequestCommandService,
    private val opportunityQueryService: OpportunityQueryService,
) {
    @Transactional
    fun createOpportunity(request: OpportunityCreateRequest): OpportunityDetail {
        val lead = leadCommandService.createLead(request)
        return opportunityQueryService.getOpportunity(lead.partyId)
    }

    @Transactional
    fun saveRequest(
        partyId: String,
        request: OpportunityRequestInput,
    ): OpportunityDetail {
        requestCommandService.upsertRequest(partyId, request)
        return opportunityQueryService.getOpportunity(partyId)
    }

    fun getOpportunity(partyId: String): OpportunityDetail = opportunityQueryService.getOpportunity(partyId)
}
