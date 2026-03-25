package com.example.leadflow.opportunity

import com.example.leadflow.shared.ConflictException
import com.example.leadflow.workflow.WorkflowReadRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import java.math.BigDecimal

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OpportunityWorkflowTest(
    private val opportunityService: OpportunityService,
    private val workflowReadRepository: WorkflowReadRepository,
) {
    @Test
    fun `product options include demo catalog items for the opportunity brief`() {
        val products = workflowReadRepository.listProducts()

        assertTrue(products.any { it.productId == "GZ-2644" && it.displayName == "Round Gizmo" })
        assertTrue(products.any { it.productId == "WG-1111" && it.displayName == "Micro Chrome Widget" })
    }

    @Test
    fun `opportunity progresses from new to brief ready to quote ready and locks the brief`() {
        val suffix = System.currentTimeMillis().toString().takeLast(6)
        val created =
            opportunityService.createOpportunity(
                OpportunityCreateRequest(
                    firstName = "Codex$suffix",
                    lastName = "Seller",
                    email = "codex+$suffix@example.com",
                    companyName = "Codex Revenue $suffix",
                ),
            )

        assertEquals(OpportunityStage.NEW, created.stage)
        assertEquals("Add deal brief", created.nextAction)

        val briefReady =
            opportunityService.saveBrief(
                created.partyId,
                OpportunityBriefInput(
                    title = "Warehouse refresh $suffix",
                    notes = "Customer needs two launch lines for the new site.",
                    lines =
                        listOf(
                            OpportunityLineInput(
                                description = "Two round gizmos",
                                productId = "GZ-2644",
                                quantity = BigDecimal("2"),
                                unitPrice = BigDecimal("24.50"),
                            ),
                            OpportunityLineInput(
                                description = "Backup calibration pack",
                                productId = "WG-1111",
                                quantity = BigDecimal("1"),
                                unitPrice = BigDecimal("12.00"),
                            ),
                        ),
                ),
            )

        assertEquals(OpportunityStage.BRIEF_READY, briefReady.stage)
        assertEquals("Create quote", briefReady.nextAction)
        assertEquals(2, briefReady.brief?.lines?.size)

        val updatedBrief =
            opportunityService.saveBrief(
                created.partyId,
                OpportunityBriefInput(
                    title = "Warehouse refresh $suffix revised",
                    notes = "Updated after the customer narrowed the scope.",
                    lines =
                        listOf(
                            OpportunityLineInput(
                                description = "Two round gizmos",
                                productId = "GZ-2644",
                                quantity = BigDecimal("2"),
                                unitPrice = BigDecimal("24.50"),
                            ),
                        ),
                ),
            )

        assertEquals("Warehouse refresh $suffix revised", updatedBrief.brief?.title)
        assertEquals(1, updatedBrief.brief?.lines?.size)

        val quoteReady = opportunityService.createQuote(created.partyId)
        assertEquals(OpportunityStage.QUOTE_READY, quoteReady.stage)
        assertEquals("Handed off", quoteReady.nextAction)
        assertNotNull(quoteReady.latestQuoteId)
        assertEquals(true, quoteReady.brief?.isLocked)

        assertThrows(ConflictException::class.java) {
            opportunityService.saveBrief(
                created.partyId,
                OpportunityBriefInput(
                    title = "Blocked edit",
                    notes = "Should not persist after quote creation",
                    lines =
                        listOf(
                            OpportunityLineInput(
                                description = "Blocked line",
                                quantity = BigDecimal("1"),
                                unitPrice = BigDecimal("1.00"),
                            ),
                        ),
                ),
            )
        }
    }
}
