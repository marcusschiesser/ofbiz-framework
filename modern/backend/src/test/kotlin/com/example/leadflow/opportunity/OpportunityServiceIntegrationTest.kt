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

    private fun uniqueSuffix(): String = System.nanoTime().toString().takeLast(8)
}
