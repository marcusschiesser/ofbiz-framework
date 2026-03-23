package org.apache.ofbiz.migration.orderlifecycle

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderRulesTest {

    @Test
    fun `edit is allowed for pending and paid only`() {
        assertDoesNotThrow { OrderRules.requireEditable(OrderStatus.PENDING) }
        assertDoesNotThrow { OrderRules.requireEditable(OrderStatus.PAID) }
        assertThrows(OrderLifecycleException::class.java) { OrderRules.requireEditable(OrderStatus.SHIPPED) }
    }

    @Test
    fun `cancel is rejected after shipment`() {
        assertThrows(OrderLifecycleException::class.java) { OrderRules.requireCancellable(OrderStatus.SHIPPED) }
        assertThrows(OrderLifecycleException::class.java) { OrderRules.requireCancellable(OrderStatus.COMPLETED) }
    }

    @Test
    fun `refund requires captured amount`() {
        assertDoesNotThrow {
            OrderRules.requireRefundable(BigDecimal("100.00"), BigDecimal("20.00"))
        }

        assertThrows(OrderLifecycleException::class.java) {
            OrderRules.requireRefundable(BigDecimal("100.00"), BigDecimal("100.00"))
        }
    }
}
