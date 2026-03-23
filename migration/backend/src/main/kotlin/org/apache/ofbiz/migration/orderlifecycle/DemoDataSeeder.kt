package org.apache.ofbiz.migration.orderlifecycle

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class DemoDataSeeder(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun seedDemoData() {
        val existingOrders = jdbcClient.sql("select count(*) from orders")
            .query(Long::class.java)
            .single()
        if (existingOrders > 0) {
            return
        }

        val customers = listOf(
            SeedCustomer("CUST-1000", UUID.fromString("00000000-0000-0000-0000-000000000101"), "Schiesser Retail GmbH", "retail@example.com"),
            SeedCustomer("CUST-1001", UUID.fromString("00000000-0000-0000-0000-000000000102"), "Blue Harbor Stores", "ops@blueharbor.test"),
            SeedCustomer("CUST-1002", UUID.fromString("00000000-0000-0000-0000-000000000103"), "Northwind Trade", "supply@northwind.test"),
        )

        customers.forEach { customer ->
            jdbcClient.sql(
                """
                insert into customers (id, customer_number, name, email, created_at)
                values (:id, :customerNumber, :name, :email, :createdAt)
                """.trimIndent(),
            )
                .param("id", customer.id)
                .param("customerNumber", customer.customerNumber)
                .param("name", customer.name)
                .param("email", customer.email)
                .param("createdAt", ts("2026-01-03T09:00:00Z"))
                .update()
        }

        val products = listOf(
            SeedProduct(UUID.fromString("00000000-0000-0000-0000-000000000201"), "SKU-TEE-001", "Essential Tee", BigDecimal("49.90")),
            SeedProduct(UUID.fromString("00000000-0000-0000-0000-000000000202"), "SKU-HOOD-002", "Studio Hoodie", BigDecimal("89.00")),
            SeedProduct(UUID.fromString("00000000-0000-0000-0000-000000000203"), "SKU-PANT-003", "Travel Pant", BigDecimal("119.00")),
        )

        products.forEach { product ->
            jdbcClient.sql(
                """
                insert into products (id, sku, name, unit_price, currency_code, active, created_at)
                values (:id, :sku, :name, :unitPrice, 'USD', true, :createdAt)
                """.trimIndent(),
            )
                .param("id", product.id)
                .param("sku", product.sku)
                .param("name", product.name)
                .param("unitPrice", product.unitPrice)
                .param("createdAt", ts("2026-01-03T09:00:00Z"))
                .update()
        }

        seedOrder(
            id = UUID.fromString("00000000-0000-0000-0000-000000001001"),
            orderNumber = "ORD-001000",
            customer = customers[0],
            status = OrderStatus.PENDING,
            subtotal = BigDecimal("188.80"),
            paidTotal = BigDecimal.ZERO,
            refundedTotal = BigDecimal.ZERO,
            createdAt = ts("2026-02-01T09:30:00Z"),
            version = 0,
            items = listOf(
                SeedOrderItem(products[0], 2, 1),
                SeedOrderItem(products[1], 1, 2),
            ),
            events = listOf(
                SeedEvent(OrderEventType.CREATED, OrderStatus.PENDING, "demo-seed", "Order created", ts("2026-02-01T09:30:00Z"), mapOf("source" to "seed")),
            ),
        )

        seedOrder(
            id = UUID.fromString("00000000-0000-0000-0000-000000001002"),
            orderNumber = "ORD-001001",
            customer = customers[1],
            status = OrderStatus.PAID,
            subtotal = BigDecimal("119.00"),
            paidTotal = BigDecimal("119.00"),
            refundedTotal = BigDecimal.ZERO,
            createdAt = ts("2026-02-02T10:15:00Z"),
            paidAt = ts("2026-02-02T10:40:00Z"),
            paymentReference = "PAY-DEMO-1001",
            version = 1,
            items = listOf(SeedOrderItem(products[2], 1, 1)),
            events = listOf(
                SeedEvent(OrderEventType.CREATED, OrderStatus.PENDING, "demo-seed", "Order created", ts("2026-02-02T10:15:00Z"), mapOf("source" to "seed")),
                SeedEvent(OrderEventType.PAID, OrderStatus.PAID, "demo-seed", "Payment captured", ts("2026-02-02T10:40:00Z"), mapOf("amount" to "119.00", "paymentReference" to "PAY-DEMO-1001")),
            ),
        )

        seedOrder(
            id = UUID.fromString("00000000-0000-0000-0000-000000001003"),
            orderNumber = "ORD-001002",
            customer = customers[2],
            status = OrderStatus.SHIPPED,
            subtotal = BigDecimal("138.90"),
            paidTotal = BigDecimal("138.90"),
            refundedTotal = BigDecimal.ZERO,
            createdAt = ts("2026-02-03T08:45:00Z"),
            paidAt = ts("2026-02-03T09:05:00Z"),
            shippedAt = ts("2026-02-03T13:10:00Z"),
            paymentReference = "PAY-DEMO-1002",
            trackingNumber = "TRACK-1002",
            version = 2,
            items = listOf(
                SeedOrderItem(products[0], 1, 1),
                SeedOrderItem(products[1], 1, 2),
            ),
            events = listOf(
                SeedEvent(OrderEventType.CREATED, OrderStatus.PENDING, "demo-seed", "Order created", ts("2026-02-03T08:45:00Z"), mapOf("source" to "seed")),
                SeedEvent(OrderEventType.PAID, OrderStatus.PAID, "demo-seed", "Payment captured", ts("2026-02-03T09:05:00Z"), mapOf("amount" to "138.90", "paymentReference" to "PAY-DEMO-1002")),
                SeedEvent(OrderEventType.SHIPPED, OrderStatus.SHIPPED, "demo-seed", "Shipment dispatched", ts("2026-02-03T13:10:00Z"), mapOf("trackingNumber" to "TRACK-1002")),
            ),
        )

        seedOrder(
            id = UUID.fromString("00000000-0000-0000-0000-000000001004"),
            orderNumber = "ORD-001003",
            customer = customers[0],
            status = OrderStatus.COMPLETED,
            subtotal = BigDecimal("99.80"),
            paidTotal = BigDecimal("99.80"),
            refundedTotal = BigDecimal.ZERO,
            createdAt = ts("2026-02-04T11:20:00Z"),
            paidAt = ts("2026-02-04T11:45:00Z"),
            shippedAt = ts("2026-02-05T07:30:00Z"),
            completedAt = ts("2026-02-06T16:00:00Z"),
            paymentReference = "PAY-DEMO-1003",
            trackingNumber = "TRACK-1003",
            version = 3,
            items = listOf(SeedOrderItem(products[0], 2, 1)),
            events = listOf(
                SeedEvent(OrderEventType.CREATED, OrderStatus.PENDING, "demo-seed", "Order created", ts("2026-02-04T11:20:00Z"), mapOf("source" to "seed")),
                SeedEvent(OrderEventType.PAID, OrderStatus.PAID, "demo-seed", "Payment captured", ts("2026-02-04T11:45:00Z"), mapOf("amount" to "99.80", "paymentReference" to "PAY-DEMO-1003")),
                SeedEvent(OrderEventType.SHIPPED, OrderStatus.SHIPPED, "demo-seed", "Shipment dispatched", ts("2026-02-05T07:30:00Z"), mapOf("trackingNumber" to "TRACK-1003")),
                SeedEvent(OrderEventType.COMPLETED, OrderStatus.COMPLETED, "demo-seed", "Order completed", ts("2026-02-06T16:00:00Z"), emptyMap<String, String>()),
            ),
        )

        seedOrder(
            id = UUID.fromString("00000000-0000-0000-0000-000000001005"),
            orderNumber = "ORD-001004",
            customer = customers[1],
            status = OrderStatus.CANCELLED,
            subtotal = BigDecimal("89.00"),
            paidTotal = BigDecimal.ZERO,
            refundedTotal = BigDecimal.ZERO,
            createdAt = ts("2026-02-07T09:10:00Z"),
            cancelledAt = ts("2026-02-07T10:25:00Z"),
            version = 1,
            items = listOf(SeedOrderItem(products[1], 1, 1)),
            events = listOf(
                SeedEvent(OrderEventType.CREATED, OrderStatus.PENDING, "demo-seed", "Order created", ts("2026-02-07T09:10:00Z"), mapOf("source" to "seed")),
                SeedEvent(OrderEventType.CANCELLED, OrderStatus.CANCELLED, "demo-seed", "Customer cancellation", ts("2026-02-07T10:25:00Z"), emptyMap<String, String>()),
            ),
        )

        seedOrder(
            id = UUID.fromString("00000000-0000-0000-0000-000000001006"),
            orderNumber = "ORD-001005",
            customer = customers[2],
            status = OrderStatus.COMPLETED,
            subtotal = BigDecimal("257.90"),
            paidTotal = BigDecimal("257.90"),
            refundedTotal = BigDecimal("49.90"),
            createdAt = ts("2026-02-08T08:00:00Z"),
            paidAt = ts("2026-02-08T08:20:00Z"),
            shippedAt = ts("2026-02-08T14:10:00Z"),
            completedAt = ts("2026-02-10T12:45:00Z"),
            paymentReference = "PAY-DEMO-1004",
            trackingNumber = "TRACK-1004",
            version = 4,
            items = listOf(
                SeedOrderItem(products[0], 1, 1),
                SeedOrderItem(products[2], 1, 2),
                SeedOrderItem(products[1], 1, 3),
            ),
            events = listOf(
                SeedEvent(OrderEventType.CREATED, OrderStatus.PENDING, "demo-seed", "Order created", ts("2026-02-08T08:00:00Z"), mapOf("source" to "seed")),
                SeedEvent(OrderEventType.PAID, OrderStatus.PAID, "demo-seed", "Payment captured", ts("2026-02-08T08:20:00Z"), mapOf("amount" to "257.90", "paymentReference" to "PAY-DEMO-1004")),
                SeedEvent(OrderEventType.SHIPPED, OrderStatus.SHIPPED, "demo-seed", "Shipment dispatched", ts("2026-02-08T14:10:00Z"), mapOf("trackingNumber" to "TRACK-1004")),
                SeedEvent(OrderEventType.COMPLETED, OrderStatus.COMPLETED, "demo-seed", "Order completed", ts("2026-02-10T12:45:00Z"), emptyMap<String, String>()),
                SeedEvent(OrderEventType.REFUNDED, OrderStatus.COMPLETED, "demo-seed", "Size issue partial refund", ts("2026-02-11T09:30:00Z"), mapOf("amount" to "49.90")),
            ),
            refunds = listOf(SeedRefund("49.90", "demo-seed", "Size issue partial refund", ts("2026-02-11T09:30:00Z"))),
        )
    }

    private fun seedOrder(
        id: UUID,
        orderNumber: String,
        customer: SeedCustomer,
        status: OrderStatus,
        subtotal: BigDecimal,
        paidTotal: BigDecimal,
        refundedTotal: BigDecimal,
        createdAt: OffsetDateTime,
        paidAt: OffsetDateTime? = null,
        shippedAt: OffsetDateTime? = null,
        completedAt: OffsetDateTime? = null,
        cancelledAt: OffsetDateTime? = null,
        paymentReference: String? = null,
        trackingNumber: String? = null,
        version: Long,
        items: List<SeedOrderItem>,
        events: List<SeedEvent>,
        refunds: List<SeedRefund> = emptyList(),
    ) {
        jdbcClient.sql(
            """
            insert into orders (
                id, order_number, customer_id, status, notes, currency_code, subtotal, paid_total, refunded_total,
                tracking_number, payment_reference, paid_at, shipped_at, completed_at, cancelled_at, created_at, updated_at, version
            ) values (
                :id, :orderNumber, :customerId, :status, :notes, 'USD', :subtotal, :paidTotal, :refundedTotal,
                :trackingNumber, :paymentReference, :paidAt, :shippedAt, :completedAt, :cancelledAt, :createdAt, :updatedAt, :version
            )
            """.trimIndent(),
        )
            .param("id", id)
            .param("orderNumber", orderNumber)
            .param("customerId", customer.id)
            .param("status", status.name)
            .param("notes", "Seeded demo order for $orderNumber")
            .param("subtotal", subtotal)
            .param("paidTotal", paidTotal)
            .param("refundedTotal", refundedTotal)
            .param("trackingNumber", trackingNumber)
            .param("paymentReference", paymentReference)
            .param("paidAt", paidAt)
            .param("shippedAt", shippedAt)
            .param("completedAt", completedAt)
            .param("cancelledAt", cancelledAt)
            .param("createdAt", createdAt)
            .param("updatedAt", listOfNotNull(cancelledAt, completedAt, shippedAt, paidAt).lastOrNull() ?: createdAt)
            .param("version", version)
            .update()

        items.forEach { item ->
            jdbcClient.sql(
                """
                insert into order_items (
                    id, order_id, product_id, line_number, sku, product_name, quantity, unit_price, line_total
                ) values (
                    :id, :orderId, :productId, :lineNumber, :sku, :productName, :quantity, :unitPrice, :lineTotal
                )
                """.trimIndent(),
            )
                .param("id", UUID.randomUUID())
                .param("orderId", id)
                .param("productId", item.product.id)
                .param("lineNumber", item.lineNumber)
                .param("sku", item.product.sku)
                .param("productName", item.product.name)
                .param("quantity", item.quantity)
                .param("unitPrice", item.product.unitPrice)
                .param("lineTotal", item.product.unitPrice.multiply(BigDecimal.valueOf(item.quantity.toLong())))
                .update()
        }

        events.forEach { event ->
            jdbcClient.sql(
                """
                insert into order_events (
                    id, order_id, event_type, status_after, actor, reason, payload, occurred_at
                ) values (
                    :id, :orderId, :eventType, :statusAfter, :actor, :reason, cast(:payload as jsonb), :occurredAt
                )
                """.trimIndent(),
            )
                .param("id", UUID.randomUUID())
                .param("orderId", id)
                .param("eventType", event.type.name)
                .param("statusAfter", event.statusAfter?.name)
                .param("actor", event.actor)
                .param("reason", event.reason)
                .param("payload", objectMapper.writeValueAsString(event.payload))
                .param("occurredAt", event.occurredAt)
                .update()
        }

        refunds.forEach { refund ->
            jdbcClient.sql(
                """
                insert into order_refunds (id, order_id, amount, actor, reason, created_at)
                values (:id, :orderId, :amount, :actor, :reason, :createdAt)
                """.trimIndent(),
            )
                .param("id", UUID.randomUUID())
                .param("orderId", id)
                .param("amount", BigDecimal(refund.amount))
                .param("actor", refund.actor)
                .param("reason", refund.reason)
                .param("createdAt", refund.createdAt)
                .update()
        }
    }

    private fun ts(value: String): OffsetDateTime = OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC)
}

private data class SeedCustomer(
    val customerNumber: String,
    val id: UUID,
    val name: String,
    val email: String,
)

private data class SeedProduct(
    val id: UUID,
    val sku: String,
    val name: String,
    val unitPrice: BigDecimal,
)

private data class SeedOrderItem(
    val product: SeedProduct,
    val quantity: Int,
    val lineNumber: Int,
)

private data class SeedEvent(
    val type: OrderEventType,
    val statusAfter: OrderStatus?,
    val actor: String,
    val reason: String,
    val occurredAt: OffsetDateTime,
    val payload: Map<String, Any?>,
)

private data class SeedRefund(
    val amount: String,
    val actor: String,
    val reason: String,
    val createdAt: OffsetDateTime,
)
