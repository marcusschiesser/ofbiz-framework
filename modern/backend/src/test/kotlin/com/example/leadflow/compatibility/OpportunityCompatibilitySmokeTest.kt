package com.example.leadflow.compatibility

import com.example.leadflow.lead.OpportunityCreateRequest
import com.example.leadflow.opportunity.OpportunityService
import com.example.leadflow.request.OpportunityRequestInput
import com.example.leadflow.request.OpportunityRequestLineInput
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import java.math.BigDecimal
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OpportunityCompatibilitySmokeTest(
    private val opportunityService: OpportunityService,
    @param:Value("\${leadflow.ofbiz.base-url}") private val ofbizBaseUrl: String,
) {
    private val httpClient =
        HttpClient.newBuilder()
            .cookieHandler(CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    @Test
    fun `kotlin-created opportunity can be inspected in ofbiz pages when shared infrastructure is available`() {
        assumeTrue(System.getenv("LEADFLOW_COMPATIBILITY_TEST") == "true")
        assumeTrue(!System.getenv("LEADFLOW_OFBIZ_USERNAME").isNullOrBlank())
        assumeTrue(!System.getenv("LEADFLOW_OFBIZ_PASSWORD").isNullOrBlank())
        assumeTrue(canReach("$ofbizBaseUrl/partymgr/"))

        val suffix = System.currentTimeMillis().toString().takeLast(6)
        val created =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Compat$suffix",
                    lastName = "Lead",
                    email = "compat+$suffix@example.com",
                    companyName = "Compat Co $suffix",
                ),
            )

        val saved =
            opportunityService.saveRequest(
                created.partyId,
                OpportunityRequestInput(
                    name = "Compat Request $suffix",
                    description = "Created by compatibility smoke test",
                    lines =
                        listOf(
                            OpportunityRequestLineInput(
                                description = "Line",
                                quantity = BigDecimal("1"),
                                unitPrice = BigDecimal("10.00"),
                            ),
                        ),
                ),
            )

        login("partymgr")
        login("ordermgr")

        assertPageContains("/partymgr/control/findparty?partyId=${created.partyId}", created.partyId)
        assertPageContains("/ordermgr/control/FindRequest?custRequestId=${saved.request?.requestId}", saved.request?.requestId ?: "")
    }

    private fun canReach(url: String): Boolean =
        try {
            val response = httpClient.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.discarding())
            response.statusCode() in 200..399
        } catch (_: Exception) {
            false
    }

    private fun login(app: String) {
        val username = System.getenv("LEADFLOW_OFBIZ_USERNAME") ?: return
        val password = System.getenv("LEADFLOW_OFBIZ_PASSWORD") ?: return
        val body = "USERNAME=$username&PASSWORD=$password"
        val request =
            HttpRequest.newBuilder(URI.create("$ofbizBaseUrl/$app/control/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun assertPageContains(path: String, expected: String) {
        val response =
            httpClient.send(
                HttpRequest.newBuilder(URI.create("$ofbizBaseUrl$path")).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
        assertTrue(response.body().contains(expected), "Expected OFBiz page $path to contain $expected")
    }
}
