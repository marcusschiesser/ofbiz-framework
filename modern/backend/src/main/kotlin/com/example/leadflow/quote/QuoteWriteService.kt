package com.example.leadflow.quote

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.ofbiz.AuditStamp
import com.example.leadflow.ofbiz.OfbizSequenceService
import com.example.leadflow.ofbiz.OfbizTables
import com.example.leadflow.shared.ConflictException
import com.example.leadflow.shared.NotFoundException
import com.example.leadflow.workflow.WorkflowReadRepository
import java.math.BigDecimal
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuoteWriteService(
    private val dsl: DSLContext,
    private val properties: OfbizProperties,
    private val sequenceService: OfbizSequenceService,
    private val workflowReadRepository: WorkflowReadRepository,
) {

    @Transactional
    fun createQuoteForOpportunity(leadPartyId: String): QuoteDetail {
        val existingQuoteId = workflowReadRepository.findLatestQuoteIdForLead(leadPartyId)
        if (existingQuoteId != null) {
            return workflowReadRepository.getQuote(existingQuoteId)
        }

        val latestRequestId = workflowReadRepository.findLatestRequestIdForLead(leadPartyId)
            ?: throw ConflictException("Save the deal brief before creating a quote.")

        return createQuoteFromRequest(latestRequestId)
    }

    @Transactional
    fun createQuoteFromRequest(custRequestId: String): QuoteDetail {
        lockRequest(custRequestId)

        val existingQuoteId = workflowReadRepository.findQuoteIdForRequest(custRequestId)
        if (existingQuoteId != null) {
            return workflowReadRepository.getQuote(existingQuoteId)
        }

        val stamp = AuditStamp()
        val request = workflowReadRepository.getRequest(custRequestId)
        val quoteId = sequenceService.nextId("Quote")
        val requestTakerPartyId = resolveRequestTakerPartyId()

        dsl.insertInto(OfbizTables.Quote.TABLE)
            .set(OfbizTables.Quote.QUOTE_ID, quoteId)
            .set(OfbizTables.Quote.QUOTE_TYPE_ID, properties.defaultQuoteTypeId)
            .set(OfbizTables.Quote.PARTY_ID, request.leadPartyId)
            .set(OfbizTables.Quote.ISSUE_DATE, stamp.now)
            .set(OfbizTables.Quote.STATUS_ID, properties.defaultQuoteStatusId)
            .set(OfbizTables.Quote.CURRENCY_UOM_ID, properties.currencyUomId)
            .set(OfbizTables.Quote.PRODUCT_STORE_ID, properties.productStoreId)
            .set(OfbizTables.Quote.SALES_CHANNEL_ENUM_ID, properties.salesChannelEnumId)
            .set(OfbizTables.Quote.VALID_FROM_DATE, stamp.now)
            .set(OfbizTables.Quote.VALID_THRU_DATE, stamp.now.plusDays(30))
            .set(OfbizTables.Quote.QUOTE_NAME, request.name)
            .set(OfbizTables.Quote.DESCRIPTION, request.description)
            .set(OfbizTables.Quote.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.Quote.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.Quote.CREATED_STAMP, stamp.now)
            .set(OfbizTables.Quote.CREATED_TX_STAMP, stamp.now)
            .execute()

        dsl.insertInto(OfbizTables.QuoteRole.TABLE)
            .set(OfbizTables.QuoteRole.QUOTE_ID, quoteId)
            .set(OfbizTables.QuoteRole.PARTY_ID, requestTakerPartyId)
            .set(OfbizTables.QuoteRole.ROLE_TYPE_ID, "REQ_TAKER")
            .set(OfbizTables.QuoteRole.FROM_DATE, stamp.now)
            .set(OfbizTables.QuoteRole.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.QuoteRole.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.QuoteRole.CREATED_STAMP, stamp.now)
            .set(OfbizTables.QuoteRole.CREATED_TX_STAMP, stamp.now)
            .execute()

        val requestRoles = dsl.resultQuery(
            """
            select party_id, role_type_id
            from cust_request_party
            where cust_request_id = ?
            order by party_id, role_type_id
            """,
            custRequestId,
        ).fetch()

        requestRoles.forEach { record ->
            dsl.insertInto(OfbizTables.QuoteRole.TABLE)
                .set(OfbizTables.QuoteRole.QUOTE_ID, quoteId)
                .set(OfbizTables.QuoteRole.PARTY_ID, record.get("party_id", String::class.java))
                .set(OfbizTables.QuoteRole.ROLE_TYPE_ID, record.get("role_type_id", String::class.java))
                .set(OfbizTables.QuoteRole.FROM_DATE, stamp.now)
                .set(OfbizTables.QuoteRole.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.QuoteRole.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.QuoteRole.CREATED_STAMP, stamp.now)
                .set(OfbizTables.QuoteRole.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        request.items.forEach { item ->
            val quoteItemSeqId = sequenceService.nextSubSequence(
                tableName = "quote_item",
                sequenceColumn = "quote_item_seq_id",
                matchColumns = mapOf("quote_id" to quoteId),
            )

            dsl.insertInto(OfbizTables.QuoteItem.TABLE)
                .set(OfbizTables.QuoteItem.QUOTE_ID, quoteId)
                .set(OfbizTables.QuoteItem.QUOTE_ITEM_SEQ_ID, quoteItemSeqId)
                .set(OfbizTables.QuoteItem.PRODUCT_ID, item.productId)
                .set(OfbizTables.QuoteItem.CUST_REQUEST_ID, custRequestId)
                .set(OfbizTables.QuoteItem.CUST_REQUEST_ITEM_SEQ_ID, item.seqId)
                .set(OfbizTables.QuoteItem.QUANTITY, item.quantity)
                .set(OfbizTables.QuoteItem.SELECTED_AMOUNT, BigDecimal.ZERO)
                .set(OfbizTables.QuoteItem.QUOTE_UNIT_PRICE, item.unitPrice)
                .set(OfbizTables.QuoteItem.COMMENTS, item.description)
                .set(OfbizTables.QuoteItem.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.QuoteItem.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.QuoteItem.CREATED_STAMP, stamp.now)
                .set(OfbizTables.QuoteItem.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        return workflowReadRepository.getQuote(quoteId)
    }

    private fun lockRequest(custRequestId: String) {
        val locked =
            dsl.resultQuery(
                """
                select cust_request_id
                from cust_request
                where cust_request_id = ?
                for update
                """,
                custRequestId,
            ).fetchOne(0, String::class.java)

        if (locked == null) {
            throw NotFoundException("Request $custRequestId was not found")
        }
    }

    private fun resolveRequestTakerPartyId(): String =
        dsl.select(OfbizTables.UserLogin.PARTY_ID)
            .from(OfbizTables.UserLogin.TABLE)
            .where(OfbizTables.UserLogin.USER_LOGIN_ID.eq(properties.createdByUserLoginId))
            .fetchOne(OfbizTables.UserLogin.PARTY_ID)
            ?: throw NotFoundException(
                "User login ${properties.createdByUserLoginId} is not linked to a party and cannot create quote roles.",
            )
}
