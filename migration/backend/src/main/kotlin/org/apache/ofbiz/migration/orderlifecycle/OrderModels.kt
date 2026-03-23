package org.apache.ofbiz.migration.orderlifecycle

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELLED,
}

enum class OrderEventType {
    CREATED,
    UPDATED,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
}

data class MoneyDto(
    val amount: BigDecimal,
    val currencyCode: String,
)

data class CustomerDto(
    val id: UUID,
    val customerNumber: String,
    val name: String,
    val email: String,
)

data class ProductDto(
    val id: UUID,
    val sku: String,
    val name: String,
    val unitPrice: MoneyDto,
    val active: Boolean,
)

data class OrderItemDto(
    val id: UUID,
    val lineNumber: Int,
    val productId: UUID?,
    val sku: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: MoneyDto,
    val lineTotal: MoneyDto,
)

data class OrderEventDto(
    val id: UUID,
    val eventType: OrderEventType,
    val statusAfter: OrderStatus?,
    val actor: String,
    val reason: String?,
    val payload: JsonNode,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val occurredAt: OffsetDateTime,
)

data class OrderRefundDto(
    val id: UUID,
    val amount: MoneyDto,
    val actor: String,
    val reason: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val createdAt: OffsetDateTime,
)

data class OrderSummaryDto(
    val id: UUID,
    val orderNumber: String,
    val version: Long,
    val status: OrderStatus,
    val customer: CustomerDto,
    val subtotal: MoneyDto,
    val paidTotal: MoneyDto,
    val refundedTotal: MoneyDto,
    val outstandingTotal: MoneyDto,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val createdAt: OffsetDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val updatedAt: OffsetDateTime,
)

data class OrderDetailDto(
    val id: UUID,
    val orderNumber: String,
    val version: Long,
    val status: OrderStatus,
    val customer: CustomerDto,
    val notes: String?,
    val subtotal: MoneyDto,
    val paidTotal: MoneyDto,
    val refundedTotal: MoneyDto,
    val outstandingTotal: MoneyDto,
    val trackingNumber: String?,
    val paymentReference: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val createdAt: OffsetDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val updatedAt: OffsetDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val paidAt: OffsetDateTime?,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val shippedAt: OffsetDateTime?,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val completedAt: OffsetDateTime?,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val cancelledAt: OffsetDateTime?,
    val canEdit: Boolean,
    val canCancel: Boolean,
    val canRefund: Boolean,
    val canShip: Boolean,
    val canComplete: Boolean,
    val items: List<OrderItemDto>,
    val events: List<OrderEventDto>,
    val refunds: List<OrderRefundDto>,
)

data class OrderListResponse(
    val data: List<OrderSummaryDto>,
)

data class ReferenceDataResponse(
    val customers: List<CustomerDto>,
    val products: List<ProductDto>,
)

data class CreateOrderRequest(
    @field:NotNull
    val customerId: UUID?,
    @field:Size(max = 2000)
    val notes: String? = null,
    @field:NotBlank
    val actor: String = "demo-operator",
    @field:NotEmpty
    @field:Valid
    val items: List<CreateOrderItemRequest> = emptyList(),
)

data class CreateOrderItemRequest(
    @field:NotNull
    val productId: UUID?,
    @field:Min(1)
    val quantity: Int,
)

data class UpdateOrderRequest(
    @field:NotNull
    val version: Long?,
    @field:Size(max = 2000)
    val notes: String? = null,
    @field:NotBlank
    val actor: String = "demo-operator",
    @field:Size(max = 250)
    val reason: String? = null,
    @field:Size(min = 1)
    @field:Valid
    val items: List<CreateOrderItemRequest>? = null,
)

data class OrderActionRequest(
    @field:NotNull
    val version: Long?,
    @field:NotBlank
    val actor: String = "demo-operator",
    @field:Size(max = 250)
    val reason: String? = null,
    @field:DecimalMin("0.00")
    val amount: BigDecimal? = null,
    @field:Size(max = 120)
    val paymentReference: String? = null,
    @field:Size(max = 120)
    val trackingNumber: String? = null,
)

data class RefundOrderRequest(
    @field:NotNull
    val version: Long?,
    @field:NotBlank
    val actor: String = "demo-operator",
    @field:NotBlank
    @field:Size(max = 250)
    val reason: String,
    @field:NotNull
    @field:DecimalMin("0.01")
    val amount: BigDecimal?,
)

data class OrderProjection(
    val id: UUID,
    val orderNumber: String,
    val customerId: UUID,
    val customerNumber: String,
    val customerName: String,
    val customerEmail: String,
    val status: OrderStatus,
    val notes: String?,
    val currencyCode: String,
    val subtotal: BigDecimal,
    val paidTotal: BigDecimal,
    val refundedTotal: BigDecimal,
    val trackingNumber: String?,
    val paymentReference: String?,
    val paidAt: OffsetDateTime?,
    val shippedAt: OffsetDateTime?,
    val completedAt: OffsetDateTime?,
    val cancelledAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val version: Long,
)

class OrderLifecycleException(
    val status: HttpStatus,
    val title: String,
    override val message: String,
) : RuntimeException(message)

object OrderRules {
    fun requireVersion(expected: Long, actual: Long) {
        if (expected != actual) {
            throw OrderLifecycleException(
                status = HttpStatus.CONFLICT,
                title = "Version conflict",
                message = "The order was modified by another request. Refresh and try again.",
            )
        }
    }

    fun requireEditable(status: OrderStatus) {
        if (status !in setOf(OrderStatus.PENDING, OrderStatus.PAID)) {
            throw invalidTransition("Only pending or paid orders can be updated.")
        }
    }

    fun requireCancellable(status: OrderStatus) {
        if (status !in setOf(OrderStatus.PENDING, OrderStatus.PAID)) {
            throw invalidTransition("Only pending or paid orders can be cancelled.")
        }
    }

    fun requirePayable(status: OrderStatus) {
        if (status != OrderStatus.PENDING) {
            throw invalidTransition("Only pending orders can be marked as paid.")
        }
    }

    fun requireShippable(status: OrderStatus) {
        if (status != OrderStatus.PAID) {
            throw invalidTransition("Only paid orders can be shipped.")
        }
    }

    fun requireCompletable(status: OrderStatus) {
        if (status != OrderStatus.SHIPPED) {
            throw invalidTransition("Only shipped orders can be completed.")
        }
    }

    fun requireRefundable(paidTotal: BigDecimal, refundedTotal: BigDecimal) {
        if (paidTotal <= refundedTotal) {
            throw OrderLifecycleException(
                status = HttpStatus.UNPROCESSABLE_ENTITY,
                title = "Refund not allowed",
                message = "Refunds are only allowed when paid total exceeds refunded total.",
            )
        }
    }

    private fun invalidTransition(message: String): OrderLifecycleException {
        return OrderLifecycleException(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Invalid lifecycle transition",
            message = message,
        )
    }
}
