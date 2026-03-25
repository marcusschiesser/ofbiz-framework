package com.example.leadflow.request

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.lead.LeadWriteService
import com.example.leadflow.ofbiz.AuditStamp
import com.example.leadflow.ofbiz.OfbizSequenceService
import com.example.leadflow.ofbiz.OfbizTables
import com.example.leadflow.shared.ConflictException
import com.example.leadflow.workflow.WorkflowReadRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class RequestWriteService(
    private val dsl: DSLContext,
    private val properties: OfbizProperties,
    private val sequenceService: OfbizSequenceService,
    private val leadWriteService: LeadWriteService,
    private val workflowReadRepository: WorkflowReadRepository,
) {
    @Transactional
    fun upsertRequest(
        leadPartyId: String,
        request: RequestCreateRequest,
    ): RequestDetail {
        val existingQuoteId = workflowReadRepository.findLatestQuoteIdForLead(leadPartyId)
        if (existingQuoteId != null) {
            throw ConflictException(
                "This opportunity already has a quote. Editing the brief after pricing is prepared is not supported in v1.",
            )
        }

        val latestRequestId = workflowReadRepository.findLatestRequestIdForLead(leadPartyId)
        return if (latestRequestId == null) {
            createRequest(leadPartyId, request)
        } else {
            updateRequest(latestRequestId, leadPartyId, request)
        }
    }

    @Transactional
    fun createRequest(
        leadPartyId: String,
        request: RequestCreateRequest,
    ): RequestDetail {
        val lead = leadWriteService.requireLead(leadPartyId)
        val stamp = AuditStamp()
        val custRequestId = sequenceService.nextId("CustRequest")

        dsl
            .insertInto(OfbizTables.CustRequest.TABLE)
            .set(OfbizTables.CustRequest.CUST_REQUEST_ID, custRequestId)
            .set(OfbizTables.CustRequest.CUST_REQUEST_TYPE_ID, properties.defaultRequestTypeId)
            .set(OfbizTables.CustRequest.STATUS_ID, properties.defaultRequestStatusId)
            .set(OfbizTables.CustRequest.FROM_PARTY_ID, lead.partyId)
            .set(OfbizTables.CustRequest.PRIORITY, BigDecimal.ONE)
            .set(OfbizTables.CustRequest.CUST_REQUEST_DATE, stamp.now)
            .set(OfbizTables.CustRequest.CUST_REQUEST_NAME, request.name.trim())
            .set(OfbizTables.CustRequest.DESCRIPTION, request.description)
            .set(OfbizTables.CustRequest.MAXIMUM_AMOUNT_UOM_ID, properties.currencyUomId)
            .set(OfbizTables.CustRequest.PRODUCT_STORE_ID, properties.productStoreId)
            .set(OfbizTables.CustRequest.SALES_CHANNEL_ENUM_ID, properties.salesChannelEnumId)
            .set(OfbizTables.CustRequest.CURRENCY_UOM_ID, properties.currencyUomId)
            .set(OfbizTables.CustRequest.CREATED_DATE, stamp.now)
            .set(OfbizTables.CustRequest.CREATED_BY_USER_LOGIN, properties.createdByUserLoginId)
            .set(OfbizTables.CustRequest.LAST_MODIFIED_DATE, stamp.now)
            .set(OfbizTables.CustRequest.LAST_MODIFIED_BY_USER_LOGIN, properties.createdByUserLoginId)
            .set(OfbizTables.CustRequest.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequest.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.CustRequest.CREATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequest.CREATED_TX_STAMP, stamp.now)
            .execute()

        dsl
            .insertInto(OfbizTables.CustRequestStatus.TABLE)
            .set(OfbizTables.CustRequestStatus.CUST_REQUEST_STATUS_ID, sequenceService.nextId("CustRequestStatus"))
            .set(OfbizTables.CustRequestStatus.STATUS_ID, properties.defaultRequestStatusId)
            .set(OfbizTables.CustRequestStatus.CUST_REQUEST_ID, custRequestId)
            .set(OfbizTables.CustRequestStatus.STATUS_DATE, stamp.now)
            .set(OfbizTables.CustRequestStatus.CHANGE_BY_USER_LOGIN_ID, properties.createdByUserLoginId)
            .set(OfbizTables.CustRequestStatus.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequestStatus.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.CustRequestStatus.CREATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequestStatus.CREATED_TX_STAMP, stamp.now)
            .execute()

        dsl
            .insertInto(OfbizTables.CustRequestParty.TABLE)
            .set(OfbizTables.CustRequestParty.CUST_REQUEST_ID, custRequestId)
            .set(OfbizTables.CustRequestParty.PARTY_ID, lead.partyId)
            .set(OfbizTables.CustRequestParty.ROLE_TYPE_ID, "LEAD")
            .set(OfbizTables.CustRequestParty.FROM_DATE, stamp.now)
            .set(OfbizTables.CustRequestParty.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequestParty.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.CustRequestParty.CREATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequestParty.CREATED_TX_STAMP, stamp.now)
            .execute()

        if (!lead.companyPartyId.isNullOrBlank()) {
            dsl
                .insertInto(OfbizTables.CustRequestParty.TABLE)
                .set(OfbizTables.CustRequestParty.CUST_REQUEST_ID, custRequestId)
                .set(OfbizTables.CustRequestParty.PARTY_ID, lead.companyPartyId)
                .set(OfbizTables.CustRequestParty.ROLE_TYPE_ID, "ACCOUNT_LEAD")
                .set(OfbizTables.CustRequestParty.FROM_DATE, stamp.now)
                .set(OfbizTables.CustRequestParty.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.CustRequestParty.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.CustRequestParty.CREATED_STAMP, stamp.now)
                .set(OfbizTables.CustRequestParty.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        request.lines.forEach { line ->
            val itemSeqId =
                sequenceService.nextSubSequence(
                    tableName = "cust_request_item",
                    sequenceColumn = "cust_request_item_seq_id",
                    matchColumns = mapOf("cust_request_id" to custRequestId),
                )

            dsl
                .insertInto(OfbizTables.CustRequestItem.TABLE)
                .set(OfbizTables.CustRequestItem.CUST_REQUEST_ID, custRequestId)
                .set(OfbizTables.CustRequestItem.CUST_REQUEST_ITEM_SEQ_ID, itemSeqId)
                .set(OfbizTables.CustRequestItem.STATUS_ID, properties.defaultRequestStatusId)
                .set(OfbizTables.CustRequestItem.PRODUCT_ID, line.productId)
                .set(OfbizTables.CustRequestItem.QUANTITY, line.quantity)
                .set(OfbizTables.CustRequestItem.SELECTED_AMOUNT, BigDecimal.ZERO)
                .set(OfbizTables.CustRequestItem.MAXIMUM_AMOUNT, line.quantity.multiply(line.unitPrice))
                .set(OfbizTables.CustRequestItem.DESCRIPTION, line.description.trim())
                .set(OfbizTables.CustRequestItem.STORY, request.description ?: line.description.trim())
                .set(OfbizTables.CustRequestItem.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.CustRequestItem.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.CustRequestItem.CREATED_STAMP, stamp.now)
                .set(OfbizTables.CustRequestItem.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        return workflowReadRepository.getRequest(custRequestId)
    }

    fun requireRequest(custRequestId: String): RequestDetail = workflowReadRepository.getRequest(custRequestId)

    private fun updateRequest(
        custRequestId: String,
        leadPartyId: String,
        request: RequestCreateRequest,
    ): RequestDetail {
        leadWriteService.requireLead(leadPartyId)
        val stamp = AuditStamp()

        dsl
            .update(OfbizTables.CustRequest.TABLE)
            .set(OfbizTables.CustRequest.CUST_REQUEST_NAME, request.name.trim())
            .set(OfbizTables.CustRequest.DESCRIPTION, request.description)
            .set(OfbizTables.CustRequest.LAST_MODIFIED_DATE, stamp.now)
            .set(OfbizTables.CustRequest.LAST_MODIFIED_BY_USER_LOGIN, properties.createdByUserLoginId)
            .set(OfbizTables.CustRequest.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.CustRequest.LAST_UPDATED_TX_STAMP, stamp.now)
            .where(OfbizTables.CustRequest.CUST_REQUEST_ID.eq(custRequestId))
            .execute()

        dsl
            .deleteFrom(OfbizTables.CustRequestItem.TABLE)
            .where(OfbizTables.CustRequestItem.CUST_REQUEST_ID.eq(custRequestId))
            .execute()

        request.lines.forEach { line ->
            val itemSeqId =
                sequenceService.nextSubSequence(
                    tableName = "cust_request_item",
                    sequenceColumn = "cust_request_item_seq_id",
                    matchColumns = mapOf("cust_request_id" to custRequestId),
                )

            dsl
                .insertInto(OfbizTables.CustRequestItem.TABLE)
                .set(OfbizTables.CustRequestItem.CUST_REQUEST_ID, custRequestId)
                .set(OfbizTables.CustRequestItem.CUST_REQUEST_ITEM_SEQ_ID, itemSeqId)
                .set(OfbizTables.CustRequestItem.STATUS_ID, properties.defaultRequestStatusId)
                .set(OfbizTables.CustRequestItem.PRODUCT_ID, line.productId)
                .set(OfbizTables.CustRequestItem.QUANTITY, line.quantity)
                .set(OfbizTables.CustRequestItem.SELECTED_AMOUNT, BigDecimal.ZERO)
                .set(OfbizTables.CustRequestItem.MAXIMUM_AMOUNT, line.quantity.multiply(line.unitPrice))
                .set(OfbizTables.CustRequestItem.DESCRIPTION, line.description.trim())
                .set(OfbizTables.CustRequestItem.STORY, request.description ?: line.description.trim())
                .set(OfbizTables.CustRequestItem.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.CustRequestItem.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.CustRequestItem.CREATED_STAMP, stamp.now)
                .set(OfbizTables.CustRequestItem.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        return workflowReadRepository.getRequest(custRequestId)
    }
}
