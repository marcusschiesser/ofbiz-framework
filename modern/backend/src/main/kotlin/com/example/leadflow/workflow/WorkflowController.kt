package com.example.leadflow.workflow

import com.example.leadflow.lead.LeadCreateRequest
import com.example.leadflow.lead.LeadDetail
import com.example.leadflow.lead.LeadSummary
import com.example.leadflow.lead.LeadWriteService
import com.example.leadflow.opportunity.OpportunityBriefInput
import com.example.leadflow.opportunity.OpportunityCreateRequest
import com.example.leadflow.opportunity.OpportunityDetail
import com.example.leadflow.opportunity.OpportunityService
import com.example.leadflow.opportunity.OpportunitySummary
import com.example.leadflow.quote.QuoteDetail
import com.example.leadflow.quote.QuoteWriteService
import com.example.leadflow.request.RequestCreateRequest
import com.example.leadflow.request.RequestDetail
import com.example.leadflow.request.RequestSummary
import com.example.leadflow.request.RequestWriteService
import com.example.leadflow.salesorder.SalesOrderDetail
import com.example.leadflow.salesorder.SalesOrderWriteService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class WorkflowController(
    private val workflowReadRepository: WorkflowReadRepository,
    private val leadWriteService: LeadWriteService,
    private val requestWriteService: RequestWriteService,
    private val quoteWriteService: QuoteWriteService,
    private val salesOrderWriteService: SalesOrderWriteService,
    private val opportunityService: OpportunityService,
) {

    @GetMapping("/opportunities")
    fun listOpportunities(): List<OpportunitySummary> = opportunityService.listOpportunities()

    @GetMapping("/products")
    fun listProducts(@RequestParam(required = false) query: String?) =
        workflowReadRepository.listProducts(query)

    @GetMapping("/opportunities/{partyId}")
    fun getOpportunity(@PathVariable partyId: String): OpportunityDetail = opportunityService.getOpportunity(partyId)

    @PostMapping("/opportunities")
    fun createOpportunity(@Valid @RequestBody request: OpportunityCreateRequest): OpportunityDetail =
        opportunityService.createOpportunity(request)

    @PutMapping("/opportunities/{partyId}/brief")
    fun saveBrief(
        @PathVariable partyId: String,
        @Valid @RequestBody request: OpportunityBriefInput,
    ): OpportunityDetail = opportunityService.saveBrief(partyId, request)

    @PostMapping("/opportunities/{partyId}/quote")
    fun createOpportunityQuote(@PathVariable partyId: String): OpportunityDetail =
        opportunityService.createQuote(partyId)

    @GetMapping("/leads")
    fun listLeads(): List<LeadSummary> = workflowReadRepository.listLeads()

    @GetMapping("/leads/{partyId}")
    fun getLead(@PathVariable partyId: String): LeadDetail = workflowReadRepository.getLead(partyId)

    @GetMapping("/leads/{partyId}/requests")
    fun getLeadRequests(@PathVariable partyId: String): List<RequestSummary> =
        workflowReadRepository.getLeadRequests(partyId)

    @PostMapping("/leads")
    fun createLead(@Valid @RequestBody request: LeadCreateRequest): LeadDetail = leadWriteService.createLead(request)

    @PostMapping("/leads/{partyId}/requests")
    fun createRequest(
        @PathVariable partyId: String,
        @Valid @RequestBody request: RequestCreateRequest,
    ): RequestDetail = requestWriteService.createRequest(partyId, request)

    @GetMapping("/requests/{custRequestId}")
    fun getRequest(@PathVariable custRequestId: String): RequestDetail = workflowReadRepository.getRequest(custRequestId)

    @PostMapping("/requests/{custRequestId}/quotes")
    fun createQuote(@PathVariable custRequestId: String): QuoteDetail = quoteWriteService.createQuoteFromRequest(custRequestId)

    @GetMapping("/quotes/{quoteId}")
    fun getQuote(@PathVariable quoteId: String): QuoteDetail = workflowReadRepository.getQuote(quoteId)

    @PostMapping("/quotes/{quoteId}/sales-orders")
    fun createSalesOrder(@PathVariable quoteId: String): SalesOrderDetail =
        salesOrderWriteService.createSalesOrderFromQuote(quoteId)

    @GetMapping("/sales-orders/{orderId}")
    fun getSalesOrder(@PathVariable orderId: String): SalesOrderDetail =
        workflowReadRepository.getSalesOrder(orderId)
}
