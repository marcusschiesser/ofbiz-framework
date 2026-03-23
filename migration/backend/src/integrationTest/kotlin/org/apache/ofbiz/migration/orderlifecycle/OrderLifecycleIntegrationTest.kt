package org.apache.ofbiz.migration.orderlifecycle

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderLifecycleIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @Autowired
    lateinit var flyway: Flyway

    @Autowired
    lateinit var demoDataSeeder: DemoDataSeeder

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val httpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun setUp() {
        flyway.migrate()
        jdbcClient.sql("truncate table order_events, order_refunds, order_items, orders, products, customers cascade").update()
        demoDataSeeder.seedDemoData()
    }

    @Test
    fun `list endpoint returns seeded orders`() {
        val response = request("GET", "/api/orders")
        val payload = readJson(response.body())

        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode())
        org.junit.jupiter.api.Assertions.assertEquals(6, payload["data"].size())
    }

    @Test
    fun `list endpoint supports combined query and status filters`() {
        val response = request("GET", "/api/orders?query=blue&status=CANCELLED")
        val payload = readJson(response.body())

        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode())
        org.junit.jupiter.api.Assertions.assertEquals(1, payload["data"].size())
        org.junit.jupiter.api.Assertions.assertEquals("ORD-001004", payload["data"][0]["orderNumber"].asText())
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED", payload["data"][0]["status"].asText())
    }

    @Test
    fun `create endpoint allocates a new order number after seeded demo data`() {
        val createResponse = request(
            "POST",
            "/api/orders",
            """
            {
              "customerId": "00000000-0000-0000-0000-000000000101",
              "notes": "Created from integration test",
              "actor": "integration-test",
              "items": [
                {
                  "productId": "00000000-0000-0000-0000-000000000201",
                  "quantity": 1
                }
              ]
            }
            """.trimIndent(),
        )
        val payload = readJson(createResponse.body())

        org.junit.jupiter.api.Assertions.assertEquals(200, createResponse.statusCode())
        org.junit.jupiter.api.Assertions.assertEquals("ORD-001006", payload["orderNumber"].asText())
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", payload["status"].asText())
        org.junit.jupiter.api.Assertions.assertEquals(49.90, payload["subtotal"]["amount"].asDouble(), 0.001)
    }

    @Test
    fun `order can be updated and cancelled before shipment`() {
        val orderId = "00000000-0000-0000-0000-000000001001"

        val updateResponse = request(
            "PATCH",
            "/api/orders/$orderId",
            """
            {
              "version": 0,
              "actor": "integration-test",
              "notes": "Customer requested gift wrap"
            }
            """.trimIndent(),
        )
        val updatePayload = readJson(updateResponse.body())

        org.junit.jupiter.api.Assertions.assertEquals(200, updateResponse.statusCode())
        org.junit.jupiter.api.Assertions.assertEquals("Customer requested gift wrap", updatePayload["notes"].asText())
        org.junit.jupiter.api.Assertions.assertEquals(1, updatePayload["version"].asInt())

        val cancelResponse = request(
            "POST",
            "/api/orders/$orderId/actions/cancel",
            """
            {
              "version": 1,
              "actor": "integration-test",
              "reason": "Customer changed mind"
            }
            """.trimIndent(),
        )
        val cancelPayload = readJson(cancelResponse.body())

        org.junit.jupiter.api.Assertions.assertEquals(200, cancelResponse.statusCode())
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED", cancelPayload["status"].asText())
    }

    private fun request(method: String, path: String, body: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port$path"))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)

        val request = when {
            body == null -> builder.method(method, HttpRequest.BodyPublishers.noBody()).build()
            else -> builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build()
        }

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun readJson(body: String): JsonNode = objectMapper.readTree(body)

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:18.1")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
