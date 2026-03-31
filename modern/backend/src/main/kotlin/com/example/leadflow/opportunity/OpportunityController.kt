package com.example.leadflow.opportunity

import com.example.leadflow.lead.OpportunityCreateRequest
import com.example.leadflow.request.OpportunityRequestInput
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["\${leadflow.web.allowed-origin:http://localhost:19006}"])
@RequestMapping("/api/opportunities")
class OpportunityController(
    private val opportunityService: OpportunityService,
) {
    @GetMapping
    fun listOpportunities(): OpportunityListResponse = opportunityService.listOpportunities()

    @PostMapping
    fun createOpportunity(
        @Valid @RequestBody request: OpportunityCreateRequest,
    ): OpportunityDetail = opportunityService.createOpportunity(request)

    @PutMapping("/{partyId}/request")
    fun saveRequest(
        @PathVariable partyId: String,
        @Valid @RequestBody request: OpportunityRequestInput,
    ): OpportunityDetail = opportunityService.saveRequest(partyId, request)

    @GetMapping("/{partyId}")
    fun getOpportunity(
        @PathVariable partyId: String,
    ): OpportunityDetail = opportunityService.getOpportunity(partyId)
}
