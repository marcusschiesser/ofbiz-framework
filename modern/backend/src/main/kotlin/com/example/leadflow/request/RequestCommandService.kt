package com.example.leadflow.request

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.ofbiz.AuditStamp
import com.example.leadflow.ofbiz.OfbizSequenceService
import com.example.leadflow.shared.ConflictException
import com.example.leadflow.shared.NotFoundException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class RequestCommandService(
    private val jdbcClient: JdbcClient,
    private val properties: OfbizProperties,
    private val sequenceService: OfbizSequenceService,
) {
    @Transactional
    fun upsertRequest(
        leadPartyId: String,
        request: OpportunityRequestInput,
    ): String {
        if (hasQuoteForLead(leadPartyId)) {
            throw ConflictException("This opportunity already has a quote. Editing the request after pricing is prepared is not supported.")
        }

        val requestId = latestRequestIdForLead(leadPartyId)
        return if (requestId == null) {
            createRequest(leadPartyId, request)
        } else {
            updateRequest(requestId, leadPartyId, request)
        }
    }

    private fun createRequest(
        leadPartyId: String,
        request: OpportunityRequestInput,
    ): String {
        val lead = requireLead(leadPartyId)
        val stamp = AuditStamp()
        val requestId = sequenceService.nextId("CustRequest")

        jdbcClient.sql(
            """
            insert into cust_request (
              cust_request_id, cust_request_type_id, status_id, from_party_id, priority,
              cust_request_date, cust_request_name, description, maximum_amount_uom_id, currency_uom_id,
              created_date, created_by_user_login, last_modified_date, last_modified_by_user_login,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :requestId, :typeId, :statusId, :leadPartyId, 1,
              :stamp, :name, :description, null, null,
              :stamp, :userLoginId, :stamp, :userLoginId,
              :stamp, :stamp, :stamp, :stamp
            )
            """.trimIndent(),
        ).param("requestId", requestId)
            .param("typeId", properties.defaultRequestTypeId)
            .param("statusId", properties.defaultRequestStatusId)
            .param("leadPartyId", leadPartyId)
            .param("stamp", stamp.now)
            .param("name", request.name.trim())
            .param("description", request.description?.trim()?.ifBlank { null })
            .param("userLoginId", properties.createdByUserLoginId)
            .update()

        insertRequestStatus(requestId, properties.defaultRequestStatusId, stamp)
        insertRequestParty(requestId, leadPartyId, "LEAD", stamp)
        lead.companyPartyId?.let { insertRequestParty(requestId, it, "ACCOUNT_LEAD", stamp) }

        if (!request.description.isNullOrBlank() || !request.story.isNullOrBlank()) {
            insertRequestItem(
                requestId = requestId,
                seqId = sequenceService.nextSubSequence("cust_request_item", "cust_request_item_seq_id", mapOf("cust_request_id" to requestId)),
                description = request.description?.trim(),
                productId = null,
                quantity = null,
                maximumAmount = null,
                story = request.story?.trim()?.ifBlank { null },
                stamp = stamp,
            )
        }

        request.lines.forEach { line ->
            insertRequestItem(
                requestId = requestId,
                seqId = sequenceService.nextSubSequence("cust_request_item", "cust_request_item_seq_id", mapOf("cust_request_id" to requestId)),
                description = line.description.trim(),
                productId = line.productId,
                quantity = line.quantity,
                maximumAmount = line.quantity.multiply(line.unitPrice),
                story = line.story?.trim()?.ifBlank { null },
                stamp = stamp,
            )
        }

        return requestId
    }

    private fun updateRequest(
        requestId: String,
        leadPartyId: String,
        request: OpportunityRequestInput,
    ): String {
        requireLead(leadPartyId)
        val stamp = AuditStamp()

        jdbcClient.sql(
            """
            update cust_request
            set cust_request_name = :name,
                description = :description,
                last_modified_date = :stamp,
                last_modified_by_user_login = :userLoginId,
                last_updated_stamp = :stamp,
                last_updated_tx_stamp = :stamp
            where cust_request_id = :requestId
            """.trimIndent(),
        ).param("requestId", requestId)
            .param("name", request.name.trim())
            .param("description", request.description?.trim()?.ifBlank { null })
            .param("stamp", stamp.now)
            .param("userLoginId", properties.createdByUserLoginId)
            .update()

        val existingItems = loadRequestItems(requestId)
        val headerItem = existingItems.firstOrNull { it.quantity == null && it.maximumAmount == null }
        val lineItems = existingItems.filterNot { it.quantity == null && it.maximumAmount == null }

        if (request.story != null) {
            if (headerItem != null) {
                jdbcClient.sql(
                    """
                    update cust_request_item
                    set story = :story,
                        last_updated_stamp = :stamp,
                        last_updated_tx_stamp = :stamp
                    where cust_request_id = :requestId and cust_request_item_seq_id = :seqId
                    """.trimIndent(),
                ).param("story", request.story.trim().ifBlank { null })
                    .param("stamp", stamp.now)
                    .param("requestId", requestId)
                    .param("seqId", headerItem.seqId)
                    .update()
            } else {
                insertRequestItem(
                    requestId = requestId,
                    seqId = sequenceService.nextSubSequence("cust_request_item", "cust_request_item_seq_id", mapOf("cust_request_id" to requestId)),
                    description = request.description?.trim(),
                    productId = null,
                    quantity = null,
                    maximumAmount = null,
                    story = request.story.trim().ifBlank { null },
                    stamp = stamp,
                )
            }
        }

        val existingByIndex = lineItems.withIndex().associate { it.index to it.value }
        request.lines.forEachIndexed { index, line ->
            val existing = existingByIndex[index]
            if (existing == null) {
                insertRequestItem(
                    requestId = requestId,
                    seqId = sequenceService.nextSubSequence("cust_request_item", "cust_request_item_seq_id", mapOf("cust_request_id" to requestId)),
                    description = line.description.trim(),
                    productId = line.productId,
                    quantity = line.quantity,
                    maximumAmount = line.quantity.multiply(line.unitPrice),
                    story = line.story?.trim()?.ifBlank { null },
                    stamp = stamp,
                )
            } else {
                jdbcClient.sql(
                    """
                    update cust_request_item
                    set description = :description,
                        product_id = :productId,
                        quantity = :quantity,
                        maximum_amount = :maximumAmount,
                        story = coalesce(:story, story),
                        last_updated_stamp = :stamp,
                        last_updated_tx_stamp = :stamp
                    where cust_request_id = :requestId and cust_request_item_seq_id = :seqId
                    """.trimIndent(),
                ).param("description", line.description.trim())
                    .param("productId", line.productId)
                    .param("quantity", line.quantity)
                    .param("maximumAmount", line.quantity.multiply(line.unitPrice))
                    .param("story", line.story?.trim()?.ifBlank { null })
                    .param("stamp", stamp.now)
                    .param("requestId", requestId)
                    .param("seqId", existing.seqId)
                    .update()
            }
        }

        if (request.lines.size < lineItems.size) {
            lineItems.drop(request.lines.size).forEach { item ->
                jdbcClient.sql(
                    "delete from cust_request_item where cust_request_id = :requestId and cust_request_item_seq_id = :seqId",
                ).param("requestId", requestId)
                    .param("seqId", item.seqId)
                    .update()
            }
        }

        return requestId
    }

    private fun requireLead(partyId: String): LeadQueryRecord {
        val record =
            jdbcClient.sql(
                """
                select
                  p.party_id,
                  per.first_name,
                  per.last_name,
                  pg.party_id as company_party_id,
                  pg.group_name as company_name
                from party p
                join person per on per.party_id = p.party_id
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
                    LeadQueryRecord(
                        partyId = rs.getString("party_id"),
                        companyPartyId = rs.getString("company_party_id"),
                    )
                }.optional()

        return record.orElseThrow { NotFoundException("Lead $partyId was not found") }
    }

    private fun latestRequestIdForLead(leadPartyId: String): String? =
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

    private fun hasQuoteForLead(leadPartyId: String): Boolean =
        jdbcClient.sql("select count(*) from quote where party_id = :leadPartyId")
            .param("leadPartyId", leadPartyId)
            .query(Long::class.java)
            .single() > 0L

    private fun insertRequestStatus(
        requestId: String,
        statusId: String,
        stamp: AuditStamp,
    ) {
        jdbcClient.sql(
            """
            insert into cust_request_status (
              cust_request_status_id, status_id, cust_request_id, status_date, change_by_user_login_id,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :statusEntryId, :statusId, :requestId, :stamp, :userLoginId,
              :stamp, :stamp, :stamp, :stamp
            )
            """.trimIndent(),
        ).param("statusEntryId", sequenceService.nextId("CustRequestStatus"))
            .param("statusId", statusId)
            .param("requestId", requestId)
            .param("stamp", stamp.now)
            .param("userLoginId", properties.createdByUserLoginId)
            .update()
    }

    private fun insertRequestParty(
        requestId: String,
        partyId: String,
        roleTypeId: String,
        stamp: AuditStamp,
    ) {
        jdbcClient.sql(
            """
            insert into cust_request_party (
              cust_request_id, party_id, role_type_id, from_date,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :requestId, :partyId, :roleTypeId, :fromDate,
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("requestId", requestId)
            .param("partyId", partyId)
            .param("roleTypeId", roleTypeId)
            .param("fromDate", stamp.now)
            .update()
    }

    private fun insertRequestItem(
        requestId: String,
        seqId: String,
        description: String?,
        productId: String?,
        quantity: BigDecimal?,
        maximumAmount: BigDecimal?,
        story: String?,
        stamp: AuditStamp,
    ) {
        jdbcClient.sql(
            """
            insert into cust_request_item (
              cust_request_id, cust_request_item_seq_id, status_id, product_id, quantity,
              selected_amount, maximum_amount, description, story,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :requestId, :seqId, :statusId, :productId, :quantity,
              0, :maximumAmount, :description, :story,
              :stamp, :stamp, :stamp, :stamp
            )
            """.trimIndent(),
        ).param("requestId", requestId)
            .param("seqId", seqId)
            .param("statusId", properties.defaultRequestStatusId)
            .param("productId", productId)
            .param("quantity", quantity)
            .param("maximumAmount", maximumAmount)
            .param("description", description)
            .param("story", story)
            .param("stamp", stamp.now)
            .update()
    }

    private fun loadRequestItems(requestId: String): List<RequestItemRow> =
        jdbcClient.sql(
            """
            select cust_request_item_seq_id, quantity, maximum_amount
            from cust_request_item
            where cust_request_id = :requestId
            order by cust_request_item_seq_id
            """.trimIndent(),
        ).param("requestId", requestId)
            .query { rs, _ ->
                RequestItemRow(
                    seqId = rs.getString("cust_request_item_seq_id"),
                    quantity = rs.getBigDecimal("quantity"),
                    maximumAmount = rs.getBigDecimal("maximum_amount"),
                )
            }.list()

    private data class LeadQueryRecord(
        val partyId: String,
        val companyPartyId: String?,
    )

    private data class RequestItemRow(
        val seqId: String,
        val quantity: BigDecimal?,
        val maximumAmount: BigDecimal?,
    )
}
