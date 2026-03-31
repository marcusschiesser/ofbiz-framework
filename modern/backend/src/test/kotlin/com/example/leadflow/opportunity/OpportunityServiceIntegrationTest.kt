package com.example.leadflow.opportunity

import com.example.leadflow.lead.OpportunityCreateRequest
import com.example.leadflow.request.OpportunityRequestInput
import com.example.leadflow.request.OpportunityRequestLineInput
import com.example.leadflow.shared.ConflictException
import com.example.leadflow.shared.NotFoundException
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import java.math.BigDecimal
import java.sql.Timestamp

@SpringBootTest
class OpportunityServiceIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired
    lateinit var opportunityService: OpportunityService

    @org.springframework.beans.factory.annotation.Autowired
    lateinit var jdbcClient: JdbcClient

    @org.springframework.beans.factory.annotation.Autowired
    lateinit var validator: Validator

    @Test
    fun `get opportunity returns assembled state after create and request save`() {
        val suffix = uniqueSuffix()
        val created =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Codex",
                    lastName = "Seller$suffix",
                    email = "codex+$suffix@example.com",
                    companyName = "Codex Co $suffix",
                ),
            )

        val requestReady =
            opportunityService.saveRequest(
                created.partyId,
                OpportunityRequestInput(
                    name = "Request $suffix",
                    description = "Customer needs pricing",
                    story = "Header story",
                    lines =
                        listOf(
                            OpportunityRequestLineInput(
                                description = "Two round gizmos",
                                productId = "GZ-2644",
                                quantity = BigDecimal("2"),
                                unitPrice = BigDecimal("24.50"),
                                story = "Line story",
                            ),
                        ),
                ),
            )

        val loaded = opportunityService.getOpportunity(created.partyId)

        assertEquals(OpportunityStage.REQUEST_READY, loaded.stage)
        assertEquals("Create quote", loaded.nextAction)
        assertEquals(requestReady.request, loaded.request)
        assertNotNull(loaded.request?.requestId)
        assertEquals("Header story", loaded.request?.story)
        assertEquals(1, loaded.request?.lines?.size)
    }

    @Test
    fun `saving request for missing lead fails`() {
        assertThrows(NotFoundException::class.java) {
            opportunityService.saveRequest(
                "MISSING",
                OpportunityRequestInput(
                    name = "Missing",
                    lines =
                        listOf(
                            OpportunityRequestLineInput(
                                description = "Line",
                                quantity = BigDecimal("1"),
                                unitPrice = BigDecimal("1.00"),
                            ),
                        ),
                ),
            )
        }
    }

    @Test
    fun `request is locked once quote exists`() {
        val suffix = uniqueSuffix()
        val created =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Lock",
                    lastName = "Me$suffix",
                    email = "lock+$suffix@example.com",
                ),
            )

        opportunityService.saveRequest(
            created.partyId,
            OpportunityRequestInput(
                name = "Lock Request",
                description = "Before quote",
                lines =
                    listOf(
                        OpportunityRequestLineInput(
                            description = "Line",
                            quantity = BigDecimal("1"),
                            unitPrice = BigDecimal("1.00"),
                        ),
                    ),
            ),
        )

        jdbcClient.sql("insert into quote (quote_id, party_id, status_id) values ('Q$suffix', :partyId, 'QUO_CREATED')")
            .param("partyId", created.partyId)
            .update()

        assertThrows(ConflictException::class.java) {
            opportunityService.saveRequest(
                created.partyId,
                OpportunityRequestInput(
                    name = "Blocked",
                    description = "Blocked",
                    lines =
                        listOf(
                            OpportunityRequestLineInput(
                                description = "Line",
                                quantity = BigDecimal("1"),
                                unitPrice = BigDecimal("2.00"),
                            ),
                        ),
                ),
            )
        }
    }

    @Test
    fun `create opportunity validates malformed input`() {
        val violations =
            validator.validate(
                OpportunityCreateRequest(
                    firstName = "",
                    lastName = "Lead",
                    email = "not-an-email",
                ),
            )

        assertTrue(violations.isNotEmpty())
    }

    @Test
    fun `save request validates malformed payload`() {
        val violations =
            validator.validate(
                OpportunityRequestInput(
                    name = "",
                    lines = emptyList(),
                ),
            )

        assertTrue(violations.isNotEmpty())
    }



    @Test
    fun `list opportunities returns newest first with derived stage`() {
        val oldSuffix = uniqueSuffix()
        val oldLead =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Older",
                    lastName = "Lead$oldSuffix",
                    email = "older+$oldSuffix@example.com",
                ),
            )

        opportunityService.saveRequest(
            oldLead.partyId,
            OpportunityRequestInput(
                name = "Old request",
                lines =
                    listOf(
                        OpportunityRequestLineInput(
                            description = "Old line",
                            quantity = BigDecimal("1"),
                            unitPrice = BigDecimal("3.00"),
                        ),
                    ),
            ),
        )

        val newSuffix = uniqueSuffix()
        val newLead =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Newest",
                    lastName = "Lead$newSuffix",
                    email = "new+$newSuffix@example.com",
                ),
            )

        val listed = opportunityService.listOpportunities().opportunities
        val oldIndex = listed.indexOfFirst { it.partyId == oldLead.partyId }
        val newIndex = listed.indexOfFirst { it.partyId == newLead.partyId }

        assertTrue(oldIndex >= 0)
        assertTrue(newIndex >= 0)
        assertTrue(newIndex < oldIndex, "Expected newer lead to be listed first")
        assertEquals(OpportunityStage.REQUEST_READY, listed.first { it.partyId == oldLead.partyId }.stage)
        assertEquals(OpportunityStage.NEW, listed.first { it.partyId == newLead.partyId }.stage)
    }

    @Test
    fun `list opportunities returns one row per lead when multiple active emails exist`() {
        val suffix = uniqueSuffix()
        val created =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Duplicate",
                    lastName = "Email$suffix",
                    email = "duplicate+$suffix@example.com",
                    companyName = "Dup Co $suffix",
                ),
            )

        val extraContactMechId = "CM$suffix"
        val newerFromDate = Timestamp.from(java.time.Instant.now().plusSeconds(60))
        jdbcClient.sql(
            """
            insert into contact_mech (
              contact_mech_id, contact_mech_type_id, info_string,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :contactMechId, 'EMAIL_ADDRESS', :email,
              current_timestamp, current_timestamp, current_timestamp, current_timestamp
            )
            """.trimIndent(),
        ).param("contactMechId", extraContactMechId)
            .param("email", "duplicate+latest+$suffix@example.com")
            .update()

        jdbcClient.sql(
            """
            insert into party_contact_mech (
              party_id, contact_mech_id, from_date, verified,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyId, :contactMechId, :fromDate, 'Y',
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("partyId", created.partyId)
            .param("contactMechId", extraContactMechId)
            .param("fromDate", newerFromDate)
            .update()

        jdbcClient.sql(
            """
            insert into party_contact_mech_purpose (
              party_id, contact_mech_id, contact_mech_purpose_type_id, from_date,
              last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp
            ) values (
              :partyId, :contactMechId, 'PRIMARY_EMAIL', :fromDate,
              :fromDate, :fromDate, :fromDate, :fromDate
            )
            """.trimIndent(),
        ).param("partyId", created.partyId)
            .param("contactMechId", extraContactMechId)
            .param("fromDate", newerFromDate)
            .update()

        val listed = opportunityService.listOpportunities().opportunities.filter { it.partyId == created.partyId }
        assertEquals(1, listed.size)
        assertEquals("duplicate+latest+$suffix@example.com", listed.single().email)
    }

    private fun uniqueSuffix(): String = System.nanoTime().toString().takeLast(8)
}
