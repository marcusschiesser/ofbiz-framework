package com.example.leadflow.quote

import com.example.leadflow.config.OfbizProperties
import com.example.leadflow.ofbiz.OfbizTables
import com.example.leadflow.opportunity.OpportunityBriefInput
import com.example.leadflow.opportunity.OpportunityCreateRequest
import com.example.leadflow.opportunity.OpportunityLineInput
import com.example.leadflow.opportunity.OpportunityService
import com.example.leadflow.workflow.WorkflowReadRepository
import java.math.BigDecimal
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class QuoteWriteServiceTest(
    private val dsl: DSLContext,
    private val opportunityService: OpportunityService,
    private val properties: OfbizProperties,
    private val quoteWriteService: QuoteWriteService,
    private val workflowReadRepository: WorkflowReadRepository,
) {

    @Test
    fun `quote creation stores req taker role using the user login party id`() {
        val requestId = createRequestForQuote()
        val quote = quoteWriteService.createQuoteFromRequest(requestId)

        val expectedPartyId =
            dsl.select(OfbizTables.UserLogin.PARTY_ID)
                .from(OfbizTables.UserLogin.TABLE)
                .where(OfbizTables.UserLogin.USER_LOGIN_ID.eq(properties.createdByUserLoginId))
                .fetchOne(OfbizTables.UserLogin.PARTY_ID)

        val actualPartyId =
            dsl.select(OfbizTables.QuoteRole.PARTY_ID)
                .from(OfbizTables.QuoteRole.TABLE)
                .where(OfbizTables.QuoteRole.QUOTE_ID.eq(quote.quoteId))
                .and(OfbizTables.QuoteRole.ROLE_TYPE_ID.eq("REQ_TAKER"))
                .fetchOne(OfbizTables.QuoteRole.PARTY_ID)

        assertEquals(expectedPartyId, actualPartyId)
    }

    @Test
    fun `concurrent quote creation returns the same quote for one request`() {
        val requestId = createRequestForQuote()
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val tasks =
                List(2) {
                    executor.submit(
                        Callable {
                            ready.countDown()
                            assertTrue(start.await(5, TimeUnit.SECONDS))
                            quoteWriteService.createQuoteFromRequest(requestId).quoteId
                        },
                    )
                }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()

            val quoteIds = tasks.map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, quoteIds.toSet().size)

            val persistedQuoteIds =
                dsl.resultQuery(
                    """
                    select distinct quote_id
                    from quote_item
                    where cust_request_id = ?
                      and quote_id is not null
                    order by quote_id
                    """,
                    requestId,
                ).mapNotNull { it.get("quote_id", String::class.java) }

            assertEquals(1, persistedQuoteIds.size)
            assertEquals(quoteIds.first(), persistedQuoteIds.single())
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createRequestForQuote(): String {
        val suffix = System.nanoTime().toString().takeLast(8)
        val opportunity =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Quote$suffix",
                    lastName = "Tester",
                    email = "quote+$suffix@example.com",
                    companyName = "Quote Test $suffix",
                ),
            )

        opportunityService.saveBrief(
            opportunity.partyId,
            OpportunityBriefInput(
                title = "Quote request $suffix",
                notes = "Prepared for quote creation test",
                lines = listOf(
                    OpportunityLineInput(
                        description = "Round gizmo line",
                        productId = "GZ-2644",
                        quantity = BigDecimal("2"),
                        unitPrice = BigDecimal("24.50"),
                    ),
                ),
            ),
        )

        return workflowReadRepository.findLatestRequestIdForLead(opportunity.partyId)
            ?: error("Expected request id for lead ${opportunity.partyId}")
    }
}
