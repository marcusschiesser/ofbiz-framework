package com.example.leadflow.opportunity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpportunityControllerTest {
    @LocalServerPort
    var port: Int = 0

    private val mapper = jacksonObjectMapper()

    private fun client(): WebTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `list endpoint returns opportunities payload`() {
        client().get()
            .uri("/api/opportunities")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.opportunities").isArray
    }

    @Test
    fun `create lead then fetch and save request succeeds`() {
        val createdBody =
            client().post()
                .uri("/api/opportunities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "firstName" to "Api",
                        "lastName" to "Lead",
                        "email" to "api.lead+1@example.com",
                        "companyName" to "Bergmann",
                    ),
                )
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .returnResult()
                .responseBody

        val partyId = mapper.readTree(String(createdBody ?: ByteArray(0))).path("partyId").asText()

        client().get()
            .uri("/api/opportunities/$partyId")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.partyId").isEqualTo(partyId)

        client().put()
            .uri("/api/opportunities/$partyId/request")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "name" to "Customer Request",
                    "description" to "Need pricing",
                    "story" to "Has quick turnaround",
                    "lines" to
                        listOf(
                            mapOf(
                                "description" to "Widget",
                                "quantity" to "2.00",
                                "unitPrice" to "10.00",
                            ),
                        ),
                ),
            )
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.stage").isEqualTo("REQUEST_READY")
            .jsonPath("$.request.lines.length()").isEqualTo(1)
    }

    @Test
    fun `create lead validation errors preserve ApiError shape`() {
        client().post()
            .uri("/api/opportunities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "firstName" to "",
                    "lastName" to "Bad",
                    "email" to "not-email",
                ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").isNotEmpty
    }

    @Test
    fun `save request validation errors preserve ApiError shape`() {
        val createdBody =
            client().post()
                .uri("/api/opportunities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "firstName" to "Validation",
                        "lastName" to "Target",
                        "email" to "validation.target+1@example.com",
                    ),
                )
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .returnResult()
                .responseBody

        val partyId = mapper.readTree(String(createdBody ?: ByteArray(0))).path("partyId").asText()

        client().put()
            .uri("/api/opportunities/$partyId/request")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "name" to "",
                    "lines" to emptyList<Map<String, Any>>(),
                ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").isNotEmpty
    }
}
