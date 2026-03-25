package com.example.leadflow.opportunity

import com.example.leadflow.lead.LeadCreateRequest
import com.example.leadflow.lead.LeadWriteService
import com.example.leadflow.quote.QuoteWriteService
import com.example.leadflow.request.RequestCreateRequest
import com.example.leadflow.request.RequestLineInput
import com.example.leadflow.request.RequestWriteService
import com.example.leadflow.workflow.WorkflowReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpportunityService(
    private val workflowReadRepository: WorkflowReadRepository,
    private val leadWriteService: LeadWriteService,
    private val requestWriteService: RequestWriteService,
    private val quoteWriteService: QuoteWriteService,
) {
    fun listOpportunities(): List<OpportunitySummary> = workflowReadRepository.listOpportunities()

    fun getOpportunity(partyId: String): OpportunityDetail = workflowReadRepository.getOpportunity(partyId)

    @Transactional
    fun createOpportunity(request: OpportunityCreateRequest): OpportunityDetail {
        val lead =
            leadWriteService.createLead(
                LeadCreateRequest(
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    companyName = request.companyName,
                    title = request.title,
                    dataSourceId = request.dataSourceId,
                ),
            )

        return workflowReadRepository.getOpportunity(lead.partyId)
    }

    @Transactional
    fun saveBrief(
        partyId: String,
        request: OpportunityBriefInput,
    ): OpportunityDetail {
        requestWriteService.upsertRequest(
            partyId,
            RequestCreateRequest(
                name = request.title,
                description = request.notes,
                lines =
                    request.lines.map { line ->
                        RequestLineInput(
                            description = line.description,
                            productId = line.productId,
                            quantity = line.quantity,
                            unitPrice = line.unitPrice,
                        )
                    },
            ),
        )

        return workflowReadRepository.getOpportunity(partyId)
    }

    @Transactional
    fun createQuote(partyId: String): OpportunityDetail {
        quoteWriteService.createQuoteForOpportunity(partyId)
        return workflowReadRepository.getOpportunity(partyId)
    }
}
