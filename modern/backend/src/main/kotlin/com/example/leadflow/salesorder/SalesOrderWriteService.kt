package com.example.leadflow.salesorder

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.ofbiz.AuditStamp
import com.example.leadflow.ofbiz.OfbizSequenceService
import com.example.leadflow.ofbiz.OfbizTables
import com.example.leadflow.quote.QuoteWriteService
import com.example.leadflow.workflow.WorkflowReadRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class SalesOrderWriteService(
    private val dsl: DSLContext,
    private val properties: OfbizProperties,
    private val sequenceService: OfbizSequenceService,
    private val quoteWriteService: QuoteWriteService,
    private val workflowReadRepository: WorkflowReadRepository,
) {
    @Transactional
    fun createSalesOrderFromQuote(quoteId: String): SalesOrderDetail {
        val quote = workflowReadRepository.getQuote(quoteId)
        val stamp = AuditStamp()
        val orderId = sequenceService.nextId("OrderHeader")
        val grandTotal =
            quote.items.fold(BigDecimal.ZERO) { total, item ->
                total + item.quantity.multiply(item.unitPrice)
            }

        ensurePartyRole(quote.partyId, "PLACING_CUSTOMER", stamp)
        ensurePartyRole(quote.partyId, "BILL_TO_CUSTOMER", stamp)
        ensurePartyRole(quote.partyId, "END_USER_CUSTOMER", stamp)
        ensurePartyRole(quote.partyId, "SHIP_TO_CUSTOMER", stamp)

        dsl
            .insertInto(OfbizTables.OrderHeader.TABLE)
            .set(OfbizTables.OrderHeader.ORDER_ID, orderId)
            .set(OfbizTables.OrderHeader.ORDER_TYPE_ID, properties.defaultOrderTypeId)
            .set(OfbizTables.OrderHeader.ORDER_NAME, "Sales Order for ${quote.quoteName ?: quote.quoteId}")
            .set(OfbizTables.OrderHeader.SALES_CHANNEL_ENUM_ID, quote.salesChannelEnumId ?: properties.salesChannelEnumId)
            .set(OfbizTables.OrderHeader.ORDER_DATE, stamp.now)
            .set(OfbizTables.OrderHeader.PRIORITY, "2")
            .set(OfbizTables.OrderHeader.ENTRY_DATE, stamp.now)
            .set(OfbizTables.OrderHeader.STATUS_ID, properties.defaultOrderStatusId)
            .set(OfbizTables.OrderHeader.CREATED_BY, properties.createdByUserLoginId)
            .set(OfbizTables.OrderHeader.CURRENCY_UOM, properties.currencyUomId)
            .set(OfbizTables.OrderHeader.WEB_SITE_ID, properties.webSiteId)
            .set(OfbizTables.OrderHeader.PRODUCT_STORE_ID, quote.productStoreId ?: properties.productStoreId)
            .set(OfbizTables.OrderHeader.REMAINING_SUB_TOTAL, grandTotal)
            .set(OfbizTables.OrderHeader.GRAND_TOTAL, grandTotal)
            .set(OfbizTables.OrderHeader.INVOICE_PER_SHIPMENT, "N")
            .set(OfbizTables.OrderHeader.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.OrderHeader.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.OrderHeader.CREATED_STAMP, stamp.now)
            .set(OfbizTables.OrderHeader.CREATED_TX_STAMP, stamp.now)
            .execute()

        listOf(
            properties.internalOrganizationPartyId to "BILL_FROM_VENDOR",
            quote.partyId to "BILL_TO_CUSTOMER",
            quote.partyId to "END_USER_CUSTOMER",
            quote.partyId to "PLACING_CUSTOMER",
            quote.partyId to "SHIP_TO_CUSTOMER",
        ).forEach { (partyId, roleTypeId) ->
            dsl
                .insertInto(OfbizTables.OrderRole.TABLE)
                .set(OfbizTables.OrderRole.ORDER_ID, orderId)
                .set(OfbizTables.OrderRole.PARTY_ID, partyId)
                .set(OfbizTables.OrderRole.ROLE_TYPE_ID, roleTypeId)
                .set(OfbizTables.OrderRole.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.OrderRole.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.OrderRole.CREATED_STAMP, stamp.now)
                .set(OfbizTables.OrderRole.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        dsl
            .insertInto(OfbizTables.OrderStatus.TABLE)
            .set(OfbizTables.OrderStatus.ORDER_STATUS_ID, sequenceService.nextId("OrderStatus"))
            .set(OfbizTables.OrderStatus.STATUS_ID, properties.defaultOrderStatusId)
            .set(OfbizTables.OrderStatus.ORDER_ID, orderId)
            .set(OfbizTables.OrderStatus.STATUS_DATETIME, stamp.now)
            .set(OfbizTables.OrderStatus.STATUS_USER_LOGIN, properties.createdByUserLoginId)
            .set(OfbizTables.OrderStatus.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.OrderStatus.LAST_UPDATED_TX_STAMP, stamp.now)
            .set(OfbizTables.OrderStatus.CREATED_STAMP, stamp.now)
            .set(OfbizTables.OrderStatus.CREATED_TX_STAMP, stamp.now)
            .execute()

        quote.items.forEach { item ->
            val itemSeqId =
                sequenceService.nextSubSequence(
                    tableName = "order_item",
                    sequenceColumn = "order_item_seq_id",
                    matchColumns = mapOf("order_id" to orderId),
                )

            dsl
                .insertInto(OfbizTables.OrderItem.TABLE)
                .set(OfbizTables.OrderItem.ORDER_ID, orderId)
                .set(OfbizTables.OrderItem.ORDER_ITEM_SEQ_ID, itemSeqId)
                .set(OfbizTables.OrderItem.ORDER_ITEM_TYPE_ID, "PRODUCT_ORDER_ITEM")
                .set(OfbizTables.OrderItem.PRODUCT_ID, item.productId)
                .set(OfbizTables.OrderItem.IS_PROMO, "N")
                .set(OfbizTables.OrderItem.QUOTE_ID, quoteId)
                .set(OfbizTables.OrderItem.QUOTE_ITEM_SEQ_ID, item.seqId)
                .set(OfbizTables.OrderItem.QUANTITY, item.quantity)
                .set(OfbizTables.OrderItem.SELECTED_AMOUNT, BigDecimal.ZERO)
                .set(OfbizTables.OrderItem.UNIT_PRICE, item.unitPrice)
                .set(OfbizTables.OrderItem.UNIT_LIST_PRICE, item.unitPrice)
                .set(OfbizTables.OrderItem.IS_MODIFIED_PRICE, "N")
                .set(OfbizTables.OrderItem.ITEM_DESCRIPTION, item.comments ?: item.productId ?: "Quote item ${item.seqId}")
                .set(OfbizTables.OrderItem.STATUS_ID, properties.defaultOrderItemStatusId)
                .set(OfbizTables.OrderItem.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.OrderItem.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.OrderItem.CREATED_STAMP, stamp.now)
                .set(OfbizTables.OrderItem.CREATED_TX_STAMP, stamp.now)
                .execute()

            dsl
                .insertInto(OfbizTables.OrderStatus.TABLE)
                .set(OfbizTables.OrderStatus.ORDER_STATUS_ID, sequenceService.nextId("OrderStatus"))
                .set(OfbizTables.OrderStatus.STATUS_ID, properties.defaultOrderItemStatusId)
                .set(OfbizTables.OrderStatus.ORDER_ID, orderId)
                .set(OfbizTables.OrderStatus.ORDER_ITEM_SEQ_ID, itemSeqId)
                .set(OfbizTables.OrderStatus.STATUS_DATETIME, stamp.now)
                .set(OfbizTables.OrderStatus.STATUS_USER_LOGIN, properties.createdByUserLoginId)
                .set(OfbizTables.OrderStatus.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.OrderStatus.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.OrderStatus.CREATED_STAMP, stamp.now)
                .set(OfbizTables.OrderStatus.CREATED_TX_STAMP, stamp.now)
                .execute()
        }

        dsl
            .update(OfbizTables.Quote.TABLE)
            .set(OfbizTables.Quote.STATUS_ID, "QUO_ORDERED")
            .set(OfbizTables.Quote.LAST_UPDATED_STAMP, stamp.now)
            .set(OfbizTables.Quote.LAST_UPDATED_TX_STAMP, stamp.now)
            .where(OfbizTables.Quote.QUOTE_ID.eq(quoteId))
            .execute()

        return workflowReadRepository.getSalesOrder(orderId)
    }

    private fun ensurePartyRole(
        partyId: String,
        roleTypeId: String,
        stamp: AuditStamp,
    ) {
        val exists =
            dsl.fetchExists(
                OfbizTables.PartyRole.TABLE,
                OfbizTables.PartyRole.PARTY_ID
                    .eq(partyId)
                    .and(OfbizTables.PartyRole.ROLE_TYPE_ID.eq(roleTypeId)),
            )
        if (!exists) {
            dsl
                .insertInto(OfbizTables.PartyRole.TABLE)
                .set(OfbizTables.PartyRole.PARTY_ID, partyId)
                .set(OfbizTables.PartyRole.ROLE_TYPE_ID, roleTypeId)
                .set(OfbizTables.PartyRole.LAST_UPDATED_STAMP, stamp.now)
                .set(OfbizTables.PartyRole.LAST_UPDATED_TX_STAMP, stamp.now)
                .set(OfbizTables.PartyRole.CREATED_STAMP, stamp.now)
                .set(OfbizTables.PartyRole.CREATED_TX_STAMP, stamp.now)
                .execute()
        }
    }
}
