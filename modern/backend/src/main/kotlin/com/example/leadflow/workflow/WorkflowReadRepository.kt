package com.example.leadflow.workflow

import com.example.leadflow.lead.LeadDetail
import com.example.leadflow.lead.LeadSummary
import com.example.leadflow.opportunity.OpportunityBackOfficeLinks
import com.example.leadflow.opportunity.OpportunityBriefDetail
import com.example.leadflow.opportunity.OpportunityDetail
import com.example.leadflow.opportunity.OpportunityLineDetail
import com.example.leadflow.opportunity.OpportunityQuoteItemSummary
import com.example.leadflow.opportunity.OpportunityQuoteSummary
import com.example.leadflow.opportunity.OpportunityStage
import com.example.leadflow.opportunity.OpportunitySummary
import com.example.leadflow.product.ProductOption
import com.example.leadflow.quote.QuoteDetail
import com.example.leadflow.quote.QuoteItemDetail
import com.example.leadflow.request.RequestDetail
import com.example.leadflow.request.RequestItemDetail
import com.example.leadflow.request.RequestSummary
import com.example.leadflow.salesorder.SalesOrderDetail
import com.example.leadflow.salesorder.SalesOrderItemDetail
import com.example.leadflow.shared.NotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class WorkflowReadRepository(
    private val dsl: DSLContext,
) {
    fun listProducts(query: String? = null): List<ProductOption> {
        val searchTerm = query?.trim()?.takeIf { it.isNotEmpty() }

        val queryResult =
            if (searchTerm == null) {
                dsl.resultQuery(
                    """
                    select
                      product_id,
                      internal_name,
                      product_name,
                      coalesce(nullif(trim(internal_name), ''), nullif(trim(product_name), ''), product_id) as display_name
                    from product
                    order by display_name, product_id
                    limit 100
                    """,
                )
            } else {
                dsl.resultQuery(
                    """
                    select
                      product_id,
                      internal_name,
                      product_name,
                      coalesce(nullif(trim(internal_name), ''), nullif(trim(product_name), ''), product_id) as display_name
                    from product
                    where (
                      product_id ilike '%' || ? || '%'
                      or coalesce(internal_name, '') ilike '%' || ? || '%'
                      or coalesce(product_name, '') ilike '%' || ? || '%'
                    )
                    order by display_name, product_id
                    limit 100
                    """,
                    searchTerm,
                    searchTerm,
                    searchTerm,
                )
            }

        return queryResult.map { record ->
            ProductOption(
                productId = record.get("product_id", String::class.java),
                displayName = record.get("display_name", String::class.java),
                productName = record.get("product_name", String::class.java),
                internalName = record.get("internal_name", String::class.java),
            )
        }
    }

    fun listOpportunities(): List<OpportunitySummary> =
        listLeads().map { lead ->
            val latestRequest = findLatestRequestForLead(lead.partyId)
            val latestQuote = findLatestQuoteForLead(lead.partyId)
            val stage = opportunityStage(latestRequest, latestQuote)

            OpportunitySummary(
                partyId = lead.partyId,
                displayName = lead.fullName,
                companyName = lead.companyName,
                email = lead.email,
                stage = stage,
                currentValue = currentOpportunityValue(latestRequest, latestQuote),
                nextAction = nextAction(stage),
                latestQuoteId = latestQuote?.quoteId,
            )
        }

    fun getOpportunity(partyId: String): OpportunityDetail {
        val lead = getLead(partyId)
        val latestRequest = findLatestRequestForLead(partyId)
        val latestQuote = findLatestQuoteForLead(partyId)
        val stage = opportunityStage(latestRequest, latestQuote)

        return OpportunityDetail(
            partyId = lead.partyId,
            displayName = lead.fullName,
            companyPartyId = lead.companyPartyId,
            companyName = lead.companyName,
            email = lead.email,
            stage = stage,
            currentValue = currentOpportunityValue(latestRequest, latestQuote),
            nextAction = nextAction(stage),
            latestQuoteId = latestQuote?.quoteId,
            brief = latestRequest?.toOpportunityBrief(isLocked = latestQuote != null),
            quote = latestQuote?.toOpportunityQuote(),
            backOfficeLinks =
                OpportunityBackOfficeLinks(
                    contactRecordPath = "/partymgr/control/findparty?partyId=${lead.partyId}",
                    accountRecordPath = lead.companyPartyId?.let { "/partymgr/control/findparty?partyId=$it" },
                    quoteRecordPath = latestQuote?.quoteId?.let { "/ordermgr/control/findquotes?quoteId=$it" },
                ),
        )
    }

    fun findLatestRequestIdForLead(partyId: String): String? =
        dsl
            .resultQuery(
                """
            select cust_request_id
            from cust_request
            where from_party_id = ?
            order by created_date desc nulls last, cust_request_id desc
            limit 1
            """,
                partyId,
            ).fetchOne(0, String::class.java)

    fun findLatestQuoteIdForLead(partyId: String): String? =
        dsl
            .resultQuery(
                """
            select quote_id
            from quote
            where party_id = ?
            order by created_stamp desc nulls last, quote_id desc
            limit 1
            """,
                partyId,
            ).fetchOne(0, String::class.java)

    fun findQuoteIdForRequest(custRequestId: String): String? =
        dsl
            .resultQuery(
                """
            select quote_id
            from quote_item
            where cust_request_id = ?
              and quote_id is not null
            order by quote_id desc
            limit 1
            """,
                custRequestId,
            ).fetchOne(0, String::class.java)

    fun findLatestRequestForLead(partyId: String): RequestDetail? = findLatestRequestIdForLead(partyId)?.let(::getRequest)

    fun findLatestQuoteForLead(partyId: String): QuoteDetail? = findLatestQuoteIdForLead(partyId)?.let(::getQuote)

    fun listLeads(): List<LeadSummary> =
        dsl
            .resultQuery(
                """
            select
              p.party_id,
              per.first_name,
              per.last_name,
              (
                select pg.group_name
                from party_relationship rel
                join party_group pg on pg.party_id = rel.party_id_from
                where rel.party_id_to = p.party_id
                  and rel.role_type_id_to = 'LEAD'
                  and rel.party_relationship_type_id = 'EMPLOYMENT'
                  and rel.thru_date is null
                order by rel.from_date desc
                limit 1
              ) as company_name,
              (
                select cm.info_string
                from party_contact_mech pcm
                join contact_mech cm on cm.contact_mech_id = pcm.contact_mech_id
                where pcm.party_id = p.party_id
                  and cm.contact_mech_type_id = 'EMAIL_ADDRESS'
                  and pcm.thru_date is null
                order by pcm.from_date desc
                limit 1
              ) as email,
              p.status_id,
              (
                select count(*)
                from cust_request cr
                where cr.from_party_id = p.party_id
              ) as request_count
            from party_role pr
            join party p on p.party_id = pr.party_id
            join person per on per.party_id = p.party_id
            where pr.role_type_id = 'LEAD'
            order by p.created_date desc nulls last, p.party_id desc
            """,
            ).map { record ->
                LeadSummary(
                    partyId = record.get("party_id", String::class.java),
                    fullName =
                        listOfNotNull(
                            record.get("first_name", String::class.java),
                            record.get("last_name", String::class.java),
                        ).joinToString(" "),
                    companyName = record.get("company_name", String::class.java),
                    statusId = record.get("status_id", String::class.java),
                    email = record.get("email", String::class.java),
                    requestCount = record.get("request_count", Int::class.java) ?: 0,
                )
            }

    fun getLead(partyId: String): LeadDetail =
        dsl
            .resultQuery(
                """
            select
              p.party_id,
              per.first_name,
              per.last_name,
              p.status_id,
              (
                select pg.party_id
                from party_relationship rel
                join party_group pg on pg.party_id = rel.party_id_from
                where rel.party_id_to = p.party_id
                  and rel.role_type_id_to = 'LEAD'
                  and rel.party_relationship_type_id = 'EMPLOYMENT'
                  and rel.thru_date is null
                order by rel.from_date desc
                limit 1
              ) as company_party_id,
              (
                select pg.group_name
                from party_relationship rel
                join party_group pg on pg.party_id = rel.party_id_from
                where rel.party_id_to = p.party_id
                  and rel.role_type_id_to = 'LEAD'
                  and rel.party_relationship_type_id = 'EMPLOYMENT'
                  and rel.thru_date is null
                order by rel.from_date desc
                limit 1
              ) as company_name,
              (
                select cm.info_string
                from party_contact_mech pcm
                join contact_mech cm on cm.contact_mech_id = pcm.contact_mech_id
                where pcm.party_id = p.party_id
                  and cm.contact_mech_type_id = 'EMAIL_ADDRESS'
                  and pcm.thru_date is null
                order by pcm.from_date desc
                limit 1
              ) as email,
              (
                select count(*)
                from cust_request cr
                where cr.from_party_id = p.party_id
              ) as request_count
            from party p
            join person per on per.party_id = p.party_id
            where p.party_id = ?
            """,
                partyId,
            ).fetchOne { record ->
                LeadDetail(
                    partyId = record.get("party_id", String::class.java),
                    fullName =
                        listOfNotNull(
                            record.get("first_name", String::class.java),
                            record.get("last_name", String::class.java),
                        ).joinToString(" "),
                    companyPartyId = record.get("company_party_id", String::class.java),
                    companyName = record.get("company_name", String::class.java),
                    statusId = record.get("status_id", String::class.java),
                    email = record.get("email", String::class.java),
                    requestCount = record.get("request_count", Int::class.java) ?: 0,
                )
            } ?: throw NotFoundException("Lead $partyId was not found")

    fun getLeadRequests(partyId: String): List<RequestSummary> =
        dsl
            .resultQuery(
                """
            select
              cr.cust_request_id,
              cr.cust_request_name,
              cr.status_id,
              (
                select count(*)
                from cust_request_item cri
                where cri.cust_request_id = cr.cust_request_id
              ) as line_count
            from cust_request cr
            where cr.from_party_id = ?
            order by cr.created_date desc nulls last, cr.cust_request_id desc
            """,
                partyId,
            ).map { record ->
                RequestSummary(
                    custRequestId = record.get("cust_request_id", String::class.java),
                    name = record.get("cust_request_name", String::class.java),
                    statusId = record.get("status_id", String::class.java),
                    lineCount = record.get("line_count", Int::class.java) ?: 0,
                )
            }

    fun getRequest(custRequestId: String): RequestDetail {
        val header =
            dsl
                .resultQuery(
                    """
            select
              cust_request_id,
              cust_request_name,
              description,
              status_id,
              from_party_id
            from cust_request
            where cust_request_id = ?
            """,
                    custRequestId,
                ).fetchOne() ?: throw NotFoundException("Request $custRequestId was not found")

        val items =
            dsl
                .resultQuery(
                    """
            select
              cust_request_item_seq_id,
              description,
              product_id,
              quantity,
              case
                when quantity is null or quantity = 0 then maximum_amount
                else coalesce(maximum_amount / quantity, 0)
              end as unit_price,
              status_id
            from cust_request_item
            where cust_request_id = ?
            order by cust_request_item_seq_id
            """,
                    custRequestId,
                ).map { record ->
                    RequestItemDetail(
                        seqId = record.get("cust_request_item_seq_id", String::class.java),
                        description = record.get("description", String::class.java) ?: "",
                        productId = record.get("product_id", String::class.java),
                        quantity = record.get("quantity", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        unitPrice = record.get("unit_price", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        statusId = record.get("status_id", String::class.java),
                    )
                }

        val quoteIds =
            dsl
                .resultQuery(
                    """
            select distinct quote_id
            from quote_item
            where cust_request_id = ?
            order by quote_id
            """,
                    custRequestId,
                ).mapNotNull { it.get("quote_id", String::class.java) }

        return RequestDetail(
            custRequestId = header.get("cust_request_id", String::class.java),
            name = header.get("cust_request_name", String::class.java),
            description = header.get("description", String::class.java),
            statusId = header.get("status_id", String::class.java),
            leadPartyId = header.get("from_party_id", String::class.java),
            items = items,
            quoteIds = quoteIds,
        )
    }

    fun getQuote(quoteId: String): QuoteDetail {
        val header =
            dsl
                .resultQuery(
                    """
            select
              quote_id,
              quote_type_id,
              status_id,
              party_id,
              quote_name,
              product_store_id,
              sales_channel_enum_id
            from quote
            where quote_id = ?
            """,
                    quoteId,
                ).fetchOne() ?: throw NotFoundException("Quote $quoteId was not found")

        val items =
            dsl
                .resultQuery(
                    """
            select
              quote_item_seq_id,
              product_id,
              quantity,
              quote_unit_price,
              cust_request_item_seq_id,
              comments
            from quote_item
            where quote_id = ?
            order by quote_item_seq_id
            """,
                    quoteId,
                ).map { record ->
                    QuoteItemDetail(
                        seqId = record.get("quote_item_seq_id", String::class.java),
                        productId = record.get("product_id", String::class.java),
                        quantity = record.get("quantity", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        unitPrice = record.get("quote_unit_price", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        sourceRequestItemSeqId = record.get("cust_request_item_seq_id", String::class.java),
                        comments = record.get("comments", String::class.java),
                    )
                }

        val orderIds =
            dsl
                .resultQuery(
                    """
            select distinct oi.order_id
            from order_item oi
            where oi.quote_id = ?
            order by oi.order_id
            """,
                    quoteId,
                ).mapNotNull { it.get("order_id", String::class.java) }

        return QuoteDetail(
            quoteId = header.get("quote_id", String::class.java),
            quoteTypeId = header.get("quote_type_id", String::class.java),
            statusId = header.get("status_id", String::class.java),
            partyId = header.get("party_id", String::class.java),
            quoteName = header.get("quote_name", String::class.java),
            productStoreId = header.get("product_store_id", String::class.java),
            salesChannelEnumId = header.get("sales_channel_enum_id", String::class.java),
            items = items,
            orderIds = orderIds,
        )
    }

    fun getSalesOrder(orderId: String): SalesOrderDetail {
        val header =
            dsl
                .resultQuery(
                    """
            select
              oh.order_id,
              oh.status_id,
              oh.order_type_id,
              oh.product_store_id,
              oh.web_site_id,
              oh.grand_total,
              (
                select oi.quote_id
                from order_item oi
                where oi.order_id = oh.order_id
                  and oi.quote_id is not null
                order by oi.order_item_seq_id
                limit 1
              ) as quote_id,
              (
                select orl.party_id
                from order_role orl
                where orl.order_id = oh.order_id
                  and orl.role_type_id = 'PLACING_CUSTOMER'
                limit 1
              ) as party_id
            from order_header oh
            where oh.order_id = ?
            """,
                    orderId,
                ).fetchOne() ?: throw NotFoundException("Sales order $orderId was not found")

        val items =
            dsl
                .resultQuery(
                    """
            select
              order_item_seq_id,
              product_id,
              item_description,
              quantity,
              unit_price,
              status_id
            from order_item
            where order_id = ?
            order by order_item_seq_id
            """,
                    orderId,
                ).map { record ->
                    SalesOrderItemDetail(
                        seqId = record.get("order_item_seq_id", String::class.java),
                        productId = record.get("product_id", String::class.java),
                        description = record.get("item_description", String::class.java),
                        quantity = record.get("quantity", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        unitPrice = record.get("unit_price", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        statusId = record.get("status_id", String::class.java),
                    )
                }

        return SalesOrderDetail(
            orderId = header.get("order_id", String::class.java),
            statusId = header.get("status_id", String::class.java),
            orderTypeId = header.get("order_type_id", String::class.java),
            partyId = header.get("party_id", String::class.java),
            quoteId = header.get("quote_id", String::class.java),
            productStoreId = header.get("product_store_id", String::class.java),
            webSiteId = header.get("web_site_id", String::class.java),
            grandTotal = header.get("grand_total", BigDecimal::class.java) ?: BigDecimal.ZERO,
            items = items,
        )
    }

    private fun opportunityStage(
        request: RequestDetail?,
        quote: QuoteDetail?,
    ): OpportunityStage =
        when {
            quote != null -> OpportunityStage.QUOTE_READY
            request != null -> OpportunityStage.BRIEF_READY
            else -> OpportunityStage.NEW
        }

    private fun nextAction(stage: OpportunityStage): String =
        when (stage) {
            OpportunityStage.NEW -> "Add deal brief"
            OpportunityStage.BRIEF_READY -> "Create quote"
            OpportunityStage.QUOTE_READY -> "Handed off"
        }

    private fun currentOpportunityValue(
        request: RequestDetail?,
        quote: QuoteDetail?,
    ): BigDecimal =
        when {
            quote != null ->
                quote.items.fold(BigDecimal.ZERO) { total, item ->
                    total + item.quantity.multiply(item.unitPrice)
                }

            request != null ->
                request.items.fold(BigDecimal.ZERO) { total, item ->
                    total + item.quantity.multiply(item.unitPrice)
                }

            else -> BigDecimal.ZERO
        }

    private fun RequestDetail.toOpportunityBrief(isLocked: Boolean): OpportunityBriefDetail =
        OpportunityBriefDetail(
            title = name,
            notes = description,
            lines =
                items.map { item ->
                    OpportunityLineDetail(
                        description = item.description,
                        productId = item.productId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                    )
                },
            isLocked = isLocked,
        )

    private fun QuoteDetail.toOpportunityQuote(): OpportunityQuoteSummary =
        OpportunityQuoteSummary(
            quoteId = quoteId,
            statusId = statusId,
            total =
                items.fold(BigDecimal.ZERO) { total, item ->
                    total + item.quantity.multiply(item.unitPrice)
                },
            items =
                items.map { item ->
                    OpportunityQuoteItemSummary(
                        seqId = item.seqId,
                        description = item.comments ?: item.productId ?: "Quote item ${item.seqId}",
                        productId = item.productId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                    )
                },
        )
}
