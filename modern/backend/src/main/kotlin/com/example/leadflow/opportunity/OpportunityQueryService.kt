package com.example.leadflow.opportunity

import com.example.leadflow.request.OpportunityRequestDetail
import com.example.leadflow.request.OpportunityRequestLineDetail
import com.example.leadflow.shared.NotFoundException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class OpportunityQueryService(
    private val jdbcClient: JdbcClient,
) {
    fun getOpportunity(partyId: String): OpportunityDetail {
        val lead =
            jdbcClient.sql(
                """
                select
                  p.party_id,
                  per.first_name,
                  per.last_name,
                  cm.info_string as email,
                  pg.party_id as company_party_id,
                  pg.group_name as company_name
                from party p
                join person per on per.party_id = p.party_id
                left join party_contact_mech_purpose pcmp
                  on pcmp.party_id = p.party_id
                 and pcmp.contact_mech_purpose_type_id = 'PRIMARY_EMAIL'
                 and pcmp.thru_date is null
                left join contact_mech cm on cm.contact_mech_id = pcmp.contact_mech_id
                left join party_relationship rel
                  on rel.party_id_to = p.party_id
                 and rel.role_type_id_to = 'LEAD'
                 and rel.role_type_id_from = 'ACCOUNT_LEAD'
                 and rel.party_relationship_type_id = 'EMPLOYMENT'
                 and rel.thru_date is null
                left join party_group pg on pg.party_id = rel.party_id_from
                where p.party_id = :partyId
                """.trimIndent(),
            ).param("partyId", partyId)
                .query { rs, _ ->
                    LeadView(
                        partyId = rs.getString("party_id"),
                        firstName = rs.getString("first_name"),
                        lastName = rs.getString("last_name"),
                        email = rs.getString("email"),
                        companyPartyId = rs.getString("company_party_id"),
                        companyName = rs.getString("company_name"),
                    )
                }.optional()
                .orElseThrow { NotFoundException("Lead $partyId was not found") }

        val requestId = latestRequestIdForLead(partyId)
        val hasQuote = hasQuoteForLead(partyId)
        val request = requestId?.let { getRequest(it, hasQuote) }
        val stage =
            when {
                hasQuote -> OpportunityStage.QUOTE_READY
                request != null -> OpportunityStage.REQUEST_READY
                else -> OpportunityStage.NEW
            }

        return OpportunityDetail(
            partyId = lead.partyId,
            displayName = "${lead.firstName} ${lead.lastName}".trim(),
            email = lead.email,
            companyPartyId = lead.companyPartyId,
            companyName = lead.companyName,
            stage = stage,
            nextAction =
                when (stage) {
                    OpportunityStage.NEW -> "Add request"
                    OpportunityStage.REQUEST_READY -> "Create quote"
                    OpportunityStage.QUOTE_READY -> "Handed off"
                },
            request = request,
        )
    }

    fun latestRequestIdForLead(leadPartyId: String): String? =
        jdbcClient.sql(
            """
            select cust_request_id
            from cust_request
            where from_party_id = :leadPartyId
            order by created_date desc, cust_request_id desc
            fetch first 1 row only
            """.trimIndent(),
        ).param("leadPartyId", leadPartyId)
            .query(String::class.java)
            .optional()
            .orElse(null)

    fun hasQuoteForLead(leadPartyId: String): Boolean =
        jdbcClient.sql("select count(*) from quote where party_id = :leadPartyId")
            .param("leadPartyId", leadPartyId)
            .query(Long::class.java)
            .single() > 0L

    private fun getRequest(
        requestId: String,
        isLocked: Boolean,
    ): OpportunityRequestDetail {
        val header =
            jdbcClient.sql(
                """
                select cust_request_id, cust_request_name, description
                from cust_request
                where cust_request_id = :requestId
                """.trimIndent(),
            ).param("requestId", requestId)
                .query { rs, _ ->
                    RequestHeader(
                        requestId = rs.getString("cust_request_id"),
                        name = rs.getString("cust_request_name"),
                        description = rs.getString("description"),
                    )
                }.single()

        val items =
            jdbcClient.sql(
                """
                select cust_request_item_seq_id, description, product_id, quantity, maximum_amount, story, status_id
                from cust_request_item
                where cust_request_id = :requestId
                order by cust_request_item_seq_id
                """.trimIndent(),
            ).param("requestId", requestId)
                .query { rs, _ ->
                    RequestItemView(
                        seqId = rs.getString("cust_request_item_seq_id"),
                        description = rs.getString("description"),
                        productId = rs.getString("product_id"),
                        quantity = rs.getBigDecimal("quantity"),
                        maximumAmount = rs.getBigDecimal("maximum_amount"),
                        story = rs.getString("story"),
                        statusId = rs.getString("status_id"),
                    )
                }.list()

        val headerItem = items.firstOrNull { it.quantity == null && it.maximumAmount == null }
        val lines =
            items.filterNot { it.quantity == null && it.maximumAmount == null }
                .map { item ->
                    OpportunityRequestLineDetail(
                        seqId = item.seqId,
                        description = item.description ?: "",
                        productId = item.productId,
                        quantity = item.quantity ?: BigDecimal.ZERO,
                        unitPrice =
                            if (item.quantity == null || item.quantity.compareTo(BigDecimal.ZERO) == 0) {
                                item.maximumAmount ?: BigDecimal.ZERO
                            } else {
                                (item.maximumAmount ?: BigDecimal.ZERO).divide(item.quantity)
                            },
                        story = item.story,
                        statusId = item.statusId,
                    )
                }

        return OpportunityRequestDetail(
            requestId = header.requestId,
            name = header.name,
            description = header.description,
            story = headerItem?.story,
            lines = lines,
            isLocked = isLocked,
        )
    }

    private data class LeadView(
        val partyId: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val companyPartyId: String?,
        val companyName: String?,
    )

    private data class RequestHeader(
        val requestId: String,
        val name: String,
        val description: String?,
    )

    private data class RequestItemView(
        val seqId: String,
        val description: String?,
        val productId: String?,
        val quantity: BigDecimal?,
        val maximumAmount: BigDecimal?,
        val story: String?,
        val statusId: String,
    )
}
