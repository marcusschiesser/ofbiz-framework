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
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.jdbc.core.simple.JdbcClient
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpportunityServiceIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired
    lateinit var opportunityService: OpportunityService

    @org.springframework.beans.factory.annotation.Autowired
    lateinit var jdbcClient: JdbcClient

    @org.springframework.beans.factory.annotation.Autowired
    lateinit var validator: Validator


    @LocalServerPort
    var port: Int = 0

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
        val suffix = uniqueSuffix()
        val first =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "First",
                    lastName = "Lead$suffix",
                    email = "first+$suffix@example.com",
                ),
            )

        val second =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Second",
                    lastName = "Lead$suffix",
                    email = "second+$suffix@example.com",
                ),
            )

        opportunityService.saveRequest(
            first.partyId,
            OpportunityRequestInput(
                name = "Need quote",
                lines =
                    listOf(
                        OpportunityRequestLineInput(
                            description = "Line",
                            quantity = BigDecimal("1"),
                            unitPrice = BigDecimal("3.00"),
                        ),
                    ),
            ),
        )

        jdbcClient.sql("insert into quote (quote_id, party_id, status_id) values (:quoteId, :partyId, 'QUO_CREATED')")
            .param("quoteId", "Q$suffix")
            .param("partyId", second.partyId)
            .update()

        val list = opportunityService.listOpportunities().items.filter { it.partyId == first.partyId || it.partyId == second.partyId }

        assertEquals(2, list.size)
        assertEquals(second.partyId, list[0].partyId)
        assertEquals(OpportunityStage.QUOTE_READY, list[0].stage)
        assertEquals("Handed off", list[0].nextAction)
        assertEquals(first.partyId, list[1].partyId)
        assertEquals(OpportunityStage.REQUEST_READY, list[1].stage)
        assertEquals("Create quote", list[1].nextAction)
    }


    @Test
    fun `controller endpoints cover list create save and validation error payloads`() {
        val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        val createPayload =
            mapOf(
                "firstName" to "Mia",
                "lastName" to "Rep",
                "email" to "mia.rep@example.com",
                "companyName" to "Bergmann Labs",
                "title" to "Buyer",
                "dataSourceId" to "INTERNAL",
            )

        val created =
            webTestClient.post().uri("/api/opportunities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createPayload)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.partyId").exists()
                .jsonPath("$.stage").isEqualTo("NEW")
                .returnResult()
                .responseBody
                ?.let { String(it, Charsets.UTF_8) }
                .orEmpty()

        val partyId = Regex("""\"partyId\"\s*:\s*\"([^\"]+)\"""").find(created)?.groupValues?.get(1)
            ?: error("partyId not found in response: $created")

        webTestClient.get().uri("/api/opportunities")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.items").isArray

        val requestPayload =
            mapOf(
                "name" to "Customer request",
                "description" to "Need a quote",
                "story" to "Urgent for next week",
                "lines" to listOf(mapOf("description" to "Widget package", "quantity" to 2, "unitPrice" to 30.5)),
            )

        webTestClient.put().uri("/api/opportunities/$partyId/request")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestPayload)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.partyId").isEqualTo(partyId)
            .jsonPath("$.stage").isEqualTo("REQUEST_READY")

        webTestClient.post().uri("/api/opportunities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("firstName" to "", "lastName" to "Rep", "email" to "bad"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").exists()

        webTestClient.put().uri("/api/opportunities/$partyId/request")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("name" to "", "lines" to emptyList<String>()))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").exists()
    }


    private fun uniqueSuffix(): String = System.nanoTime().toString().takeLast(8)
}
