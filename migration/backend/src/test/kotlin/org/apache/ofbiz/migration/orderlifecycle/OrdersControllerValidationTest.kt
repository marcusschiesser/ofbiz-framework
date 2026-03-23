package org.apache.ofbiz.migration.orderlifecycle

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class OrdersControllerValidationTest {

    private lateinit var orderService: OrderService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        orderService = org.mockito.kotlin.mock()
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }

        mockMvc = MockMvcBuilders.standaloneSetup(OrdersController(orderService))
            .setControllerAdvice(ApiExceptionHandler())
            .setMessageConverters(MappingJackson2HttpMessageConverter())
            .setValidator(validator)
            .build()
    }

    @Test
    fun `create order rejects empty items`() {
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerId": "${UUID.randomUUID()}",
                      "actor": "tester",
                      "items": []
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Validation failed"))
    }

    @Test
    fun `pay endpoint delegates when request is valid`() {
        val orderId = UUID.randomUUID()
        given(orderService.markPaid(org.mockito.kotlin.eq(orderId), org.mockito.kotlin.any())).willReturn(sampleOrder(orderId))

        mockMvc.perform(
            post("/api/orders/$orderId/actions/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "version": 0,
                      "actor": "tester",
                      "amount": 49.90,
                      "paymentReference": "PAY-1"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    private fun sampleOrder(orderId: UUID): OrderDetailDto {
        val now = OffsetDateTime.now()
        return OrderDetailDto(
            id = orderId,
            orderNumber = "ORD-TEST",
            version = 0,
            status = OrderStatus.PENDING,
            customer = CustomerDto(orderId, "CUST", "Test", "test@example.com"),
            notes = null,
            subtotal = MoneyDto(BigDecimal("49.90"), "USD"),
            paidTotal = MoneyDto(BigDecimal.ZERO, "USD"),
            refundedTotal = MoneyDto(BigDecimal.ZERO, "USD"),
            outstandingTotal = MoneyDto(BigDecimal("49.90"), "USD"),
            trackingNumber = null,
            paymentReference = null,
            createdAt = now,
            updatedAt = now,
            paidAt = null,
            shippedAt = null,
            completedAt = null,
            cancelledAt = null,
            canEdit = true,
            canCancel = true,
            canRefund = false,
            canShip = false,
            canComplete = false,
            items = emptyList(),
            events = emptyList(),
            refunds = emptyList(),
        )
    }
}
