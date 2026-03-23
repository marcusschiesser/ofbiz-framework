package org.apache.ofbiz.migration.orderlifecycle

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class OrderService(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
) {

    fun listOrders(query: String?, status: OrderStatus?): List<OrderSummaryDto> {
        val sql = buildString {
            append(
                """
                select o.id, o.order_number, o.status, o.notes, o.currency_code, o.subtotal, o.paid_total, o.refunded_total,
                       o.tracking_number, o.payment_reference, o.paid_at, o.shipped_at, o.completed_at, o.cancelled_at,
                       o.created_at, o.updated_at, o.version,
                       c.id as customer_id, c.customer_number, c.name as customer_name, c.email as customer_email
                from orders o
                join customers c on c.id = o.customer_id
                where 1 = 1
                """.trimIndent(),
            )
            if (status != null) append("\n and o.status = :status")
            if (!query.isNullOrBlank()) {
                append(
                    """
                    
                    and (
                        lower(o.order_number) like :query
                        or lower(c.customer_number) like :query
                        or lower(c.name) like :query
                        or lower(c.email) like :query
                     )
                    """.trimIndent(),
                )
            }
            append("\n order by o.created_at desc, o.order_number desc")
        }

        var spec = jdbcClient.sql(sql)
        if (status != null) {
            spec = spec.param("status", status.name)
        }
        if (!query.isNullOrBlank()) {
            spec = spec.param("query", "%${query.trim().lowercase()}%")
        }

        return spec.query(::mapOrderProjection).list().map(::toSummaryDto)
    }

    fun getOrder(orderId: UUID): OrderDetailDto {
        val projection = getOrderProjection(orderId)
        val items = getOrderItems(orderId)
        val events = getOrderEvents(orderId)
        val refunds = getOrderRefunds(orderId)

        return toDetailDto(projection, items, events, refunds)
    }

    fun getReferenceData(): ReferenceDataResponse {
        val customers = jdbcClient.sql(
            """
            select id, customer_number, name, email
            from customers
            order by customer_number
            """.trimIndent(),
        ).query { rs, _ ->
            CustomerDto(
                id = rs.getObject("id", UUID::class.java),
                customerNumber = rs.getString("customer_number"),
                name = rs.getString("name"),
                email = rs.getString("email"),
            )
        }.list()

        val products = jdbcClient.sql(
            """
            select id, sku, name, unit_price, currency_code, active
            from products
            order by sku
            """.trimIndent(),
        ).query { rs, _ ->
            ProductDto(
                id = rs.getObject("id", UUID::class.java),
                sku = rs.getString("sku"),
                name = rs.getString("name"),
                unitPrice = MoneyDto(rs.getBigDecimal("unit_price"), rs.getString("currency_code")),
                active = rs.getBoolean("active"),
            )
        }.list()

        return ReferenceDataResponse(customers = customers, products = products)
    }

    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderDetailDto {
        val customer = getCustomer(request.customerId!!)
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val items = hydrateItems(request.items)
        val subtotal = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.lineTotal.amount }.scaled()
        val orderId = UUID.randomUUID()
        val orderNumber = nextOrderNumber()

        jdbcClient.sql(
            """
            insert into orders (
                id, order_number, customer_id, status, notes, currency_code, subtotal, paid_total, refunded_total,
                tracking_number, payment_reference, paid_at, shipped_at, completed_at, cancelled_at, created_at, updated_at, version
            ) values (
                :id, :orderNumber, :customerId, :status, :notes, :currencyCode, :subtotal, 0, 0,
                null, null, null, null, null, null, :createdAt, :updatedAt, 0
            )
            """.trimIndent(),
        )
            .param("id", orderId)
            .param("orderNumber", orderNumber)
            .param("customerId", customer.id)
            .param("status", OrderStatus.PENDING.name)
            .param("notes", request.notes)
            .param("currencyCode", items.first().unitPrice.currencyCode)
            .param("subtotal", subtotal)
            .param("createdAt", now)
            .param("updatedAt", now)
            .update()

        replaceItems(orderId, items)
        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.CREATED,
            statusAfter = OrderStatus.PENDING,
            actor = request.actor,
            reason = "Order created",
            occurredAt = now,
            payload = objectMapper.valueToTree(
                mapOf(
                    "customerId" to customer.id,
                    "itemCount" to items.size,
                    "subtotal" to subtotal,
                ),
            ),
        )

        return getOrder(orderId)
    }

    @Transactional
    fun updateOrder(orderId: UUID, request: UpdateOrderRequest): OrderDetailDto {
        val current = getOrderProjection(orderId)
        OrderRules.requireVersion(request.version!!, current.version)
        OrderRules.requireEditable(current.status)

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val items = request.items?.let { hydrateItems(it) } ?: getOrderItems(orderId)
        val subtotal = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTotal.amount) }.scaled()
        val notes = request.notes ?: current.notes

        updateOrderRecord(
            orderId = orderId,
            expectedVersion = current.version,
            newStatus = current.status,
            notes = notes,
            subtotal = subtotal,
            paidTotal = current.paidTotal,
            refundedTotal = current.refundedTotal,
            trackingNumber = current.trackingNumber,
            paymentReference = current.paymentReference,
            paidAt = current.paidAt,
            shippedAt = current.shippedAt,
            completedAt = current.completedAt,
            cancelledAt = current.cancelledAt,
            updatedAt = now,
        )

        if (request.items != null) {
            replaceItems(orderId, items)
        }

        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.UPDATED,
            statusAfter = current.status,
            actor = request.actor,
            reason = request.reason ?: "Order updated",
            occurredAt = now,
            payload = objectMapper.valueToTree(
                mapOf(
                    "notesChanged" to (request.notes != null),
                    "itemsChanged" to (request.items != null),
                    "subtotal" to subtotal,
                ),
            ),
        )

        return getOrder(orderId)
    }

    @Transactional
    fun markPaid(orderId: UUID, request: OrderActionRequest): OrderDetailDto {
        val current = getOrderProjection(orderId)
        OrderRules.requireVersion(request.version!!, current.version)
        OrderRules.requirePayable(current.status)

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val amount = (request.amount ?: current.subtotal).scaled()
        if (amount.compareTo(current.subtotal) < 0) {
            throw OrderLifecycleException(
                status = HttpStatus.UNPROCESSABLE_ENTITY,
                title = "Payment amount too low",
                message = "Payment amount must cover the full order subtotal for this lifecycle flow.",
            )
        }

        updateOrderRecord(
            orderId = orderId,
            expectedVersion = current.version,
            newStatus = OrderStatus.PAID,
            notes = current.notes,
            subtotal = current.subtotal,
            paidTotal = amount,
            refundedTotal = current.refundedTotal,
            trackingNumber = current.trackingNumber,
            paymentReference = request.paymentReference ?: current.paymentReference,
            paidAt = now,
            shippedAt = current.shippedAt,
            completedAt = current.completedAt,
            cancelledAt = current.cancelledAt,
            updatedAt = now,
        )

        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.PAID,
            statusAfter = OrderStatus.PAID,
            actor = request.actor,
            reason = request.reason ?: "Order marked as paid",
            occurredAt = now,
            payload = objectMapper.valueToTree(
                mapOf(
                    "amount" to amount,
                    "paymentReference" to request.paymentReference,
                ),
            ),
        )

        return getOrder(orderId)
    }

    @Transactional
    fun markShipped(orderId: UUID, request: OrderActionRequest): OrderDetailDto {
        val current = getOrderProjection(orderId)
        OrderRules.requireVersion(request.version!!, current.version)
        OrderRules.requireShippable(current.status)

        val now = OffsetDateTime.now(ZoneOffset.UTC)

        updateOrderRecord(
            orderId = orderId,
            expectedVersion = current.version,
            newStatus = OrderStatus.SHIPPED,
            notes = current.notes,
            subtotal = current.subtotal,
            paidTotal = current.paidTotal,
            refundedTotal = current.refundedTotal,
            trackingNumber = request.trackingNumber ?: current.trackingNumber,
            paymentReference = current.paymentReference,
            paidAt = current.paidAt,
            shippedAt = now,
            completedAt = current.completedAt,
            cancelledAt = current.cancelledAt,
            updatedAt = now,
        )

        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.SHIPPED,
            statusAfter = OrderStatus.SHIPPED,
            actor = request.actor,
            reason = request.reason ?: "Order shipped",
            occurredAt = now,
            payload = objectMapper.valueToTree(mapOf("trackingNumber" to request.trackingNumber)),
        )

        return getOrder(orderId)
    }

    @Transactional
    fun markCompleted(orderId: UUID, request: OrderActionRequest): OrderDetailDto {
        val current = getOrderProjection(orderId)
        OrderRules.requireVersion(request.version!!, current.version)
        OrderRules.requireCompletable(current.status)

        val now = OffsetDateTime.now(ZoneOffset.UTC)

        updateOrderRecord(
            orderId = orderId,
            expectedVersion = current.version,
            newStatus = OrderStatus.COMPLETED,
            notes = current.notes,
            subtotal = current.subtotal,
            paidTotal = current.paidTotal,
            refundedTotal = current.refundedTotal,
            trackingNumber = current.trackingNumber,
            paymentReference = current.paymentReference,
            paidAt = current.paidAt,
            shippedAt = current.shippedAt,
            completedAt = now,
            cancelledAt = current.cancelledAt,
            updatedAt = now,
        )

        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.COMPLETED,
            statusAfter = OrderStatus.COMPLETED,
            actor = request.actor,
            reason = request.reason ?: "Order completed",
            occurredAt = now,
            payload = objectMapper.createObjectNode(),
        )

        return getOrder(orderId)
    }

    @Transactional
    fun cancelOrder(orderId: UUID, request: OrderActionRequest): OrderDetailDto {
        val current = getOrderProjection(orderId)
        OrderRules.requireVersion(request.version!!, current.version)
        OrderRules.requireCancellable(current.status)

        val now = OffsetDateTime.now(ZoneOffset.UTC)

        updateOrderRecord(
            orderId = orderId,
            expectedVersion = current.version,
            newStatus = OrderStatus.CANCELLED,
            notes = current.notes,
            subtotal = current.subtotal,
            paidTotal = current.paidTotal,
            refundedTotal = current.refundedTotal,
            trackingNumber = current.trackingNumber,
            paymentReference = current.paymentReference,
            paidAt = current.paidAt,
            shippedAt = current.shippedAt,
            completedAt = current.completedAt,
            cancelledAt = now,
            updatedAt = now,
        )

        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.CANCELLED,
            statusAfter = OrderStatus.CANCELLED,
            actor = request.actor,
            reason = request.reason ?: "Order cancelled",
            occurredAt = now,
            payload = objectMapper.createObjectNode(),
        )

        return getOrder(orderId)
    }

    @Transactional
    fun refundOrder(orderId: UUID, request: RefundOrderRequest): OrderDetailDto {
        val current = getOrderProjection(orderId)
        OrderRules.requireVersion(request.version!!, current.version)
        OrderRules.requireRefundable(current.paidTotal, current.refundedTotal)

        val amount = request.amount!!.scaled()
        if (current.refundedTotal.add(amount).compareTo(current.paidTotal) > 0) {
            throw OrderLifecycleException(
                status = HttpStatus.UNPROCESSABLE_ENTITY,
                title = "Refund amount too high",
                message = "Refund amount exceeds the captured payment total.",
            )
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        jdbcClient.sql(
            """
            insert into order_refunds (id, order_id, amount, actor, reason, created_at)
            values (:id, :orderId, :amount, :actor, :reason, :createdAt)
            """.trimIndent(),
        )
            .param("id", UUID.randomUUID())
            .param("orderId", orderId)
            .param("amount", amount)
            .param("actor", request.actor)
            .param("reason", request.reason)
            .param("createdAt", now)
            .update()

        updateOrderRecord(
            orderId = orderId,
            expectedVersion = current.version,
            newStatus = current.status,
            notes = current.notes,
            subtotal = current.subtotal,
            paidTotal = current.paidTotal,
            refundedTotal = current.refundedTotal.add(amount).scaled(),
            trackingNumber = current.trackingNumber,
            paymentReference = current.paymentReference,
            paidAt = current.paidAt,
            shippedAt = current.shippedAt,
            completedAt = current.completedAt,
            cancelledAt = current.cancelledAt,
            updatedAt = now,
        )

        insertEvent(
            orderId = orderId,
            eventType = OrderEventType.REFUNDED,
            statusAfter = current.status,
            actor = request.actor,
            reason = request.reason,
            occurredAt = now,
            payload = objectMapper.valueToTree(mapOf("amount" to amount)),
        )

        return getOrder(orderId)
    }

    fun getOrderProjection(orderId: UUID): OrderProjection {
        return jdbcClient.sql(
            """
            select o.id, o.order_number, o.status, o.notes, o.currency_code, o.subtotal, o.paid_total, o.refunded_total,
                   o.tracking_number, o.payment_reference, o.paid_at, o.shipped_at, o.completed_at, o.cancelled_at,
                   o.created_at, o.updated_at, o.version,
                   c.id as customer_id, c.customer_number, c.name as customer_name, c.email as customer_email
            from orders o
            join customers c on c.id = o.customer_id
            where o.id = :id
            """.trimIndent(),
        )
            .param("id", orderId)
            .query(::mapOrderProjection)
            .optional()
            .orElseThrow {
                OrderLifecycleException(
                    status = HttpStatus.NOT_FOUND,
                    title = "Order not found",
                    message = "No order exists for id $orderId",
                )
            }
    }

    private fun getOrderItems(orderId: UUID): List<OrderItemDto> {
        return jdbcClient.sql(
            """
            select id, line_number, product_id, sku, product_name, quantity, unit_price, line_total
            from order_items
            where order_id = :orderId
            order by line_number
            """.trimIndent(),
        )
            .param("orderId", orderId)
            .query { rs, _ ->
                OrderItemDto(
                    id = rs.getObject("id", UUID::class.java),
                    lineNumber = rs.getInt("line_number"),
                    productId = rs.getObject("product_id", UUID::class.java),
                    sku = rs.getString("sku"),
                    productName = rs.getString("product_name"),
                    quantity = rs.getInt("quantity"),
                    unitPrice = MoneyDto(rs.getBigDecimal("unit_price"), "USD"),
                    lineTotal = MoneyDto(rs.getBigDecimal("line_total"), "USD"),
                )
            }
            .list()
    }

    private fun getOrderEvents(orderId: UUID): List<OrderEventDto> {
        return jdbcClient.sql(
            """
            select id, event_type, status_after, actor, reason, payload::text as payload, occurred_at
            from order_events
            where order_id = :orderId
            order by occurred_at desc, id desc
            """.trimIndent(),
        )
            .param("orderId", orderId)
            .query { rs, _ ->
                OrderEventDto(
                    id = rs.getObject("id", UUID::class.java),
                    eventType = OrderEventType.valueOf(rs.getString("event_type")),
                    statusAfter = rs.getString("status_after")?.let(OrderStatus::valueOf),
                    actor = rs.getString("actor"),
                    reason = rs.getString("reason"),
                    payload = objectMapper.readTree(rs.getString("payload")),
                    occurredAt = rs.getObject("occurred_at", OffsetDateTime::class.java),
                )
            }
            .list()
    }

    private fun getOrderRefunds(orderId: UUID): List<OrderRefundDto> {
        return jdbcClient.sql(
            """
            select id, amount, actor, reason, created_at
            from order_refunds
            where order_id = :orderId
            order by created_at desc
            """.trimIndent(),
        )
            .param("orderId", orderId)
            .query { rs, _ ->
                OrderRefundDto(
                    id = rs.getObject("id", UUID::class.java),
                    amount = MoneyDto(rs.getBigDecimal("amount"), "USD"),
                    actor = rs.getString("actor"),
                    reason = rs.getString("reason"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
                )
            }
            .list()
    }

    private fun getCustomer(customerId: UUID): CustomerDto {
        return jdbcClient.sql(
            """
            select id, customer_number, name, email
            from customers
            where id = :id
            """.trimIndent(),
        )
            .param("id", customerId)
            .query { rs, _ ->
                CustomerDto(
                    id = rs.getObject("id", UUID::class.java),
                    customerNumber = rs.getString("customer_number"),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                )
            }
            .optional()
            .orElseThrow {
                OrderLifecycleException(
                    status = HttpStatus.NOT_FOUND,
                    title = "Customer not found",
                    message = "No customer exists for id $customerId",
                )
            }
    }

    private fun hydrateItems(requestItems: List<CreateOrderItemRequest>): List<OrderItemDto> {
        return requestItems.mapIndexed { index, item ->
            val product = jdbcClient.sql(
                """
                select id, sku, name, unit_price, currency_code, active
                from products
                where id = :id
                """.trimIndent(),
            )
                .param("id", item.productId!!)
                .query { rs, _ ->
                    ProductDto(
                        id = rs.getObject("id", UUID::class.java),
                        sku = rs.getString("sku"),
                        name = rs.getString("name"),
                        unitPrice = MoneyDto(rs.getBigDecimal("unit_price"), rs.getString("currency_code")),
                        active = rs.getBoolean("active"),
                    )
                }
                .optional()
                .orElseThrow {
                    OrderLifecycleException(
                        status = HttpStatus.NOT_FOUND,
                        title = "Product not found",
                        message = "No product exists for id ${item.productId}",
                    )
                }

            if (!product.active) {
                throw OrderLifecycleException(
                    status = HttpStatus.UNPROCESSABLE_ENTITY,
                    title = "Inactive product",
                    message = "Product ${product.sku} is not active.",
                )
            }

            val lineTotal = product.unitPrice.amount.multiply(BigDecimal.valueOf(item.quantity.toLong())).scaled()
            OrderItemDto(
                id = UUID.randomUUID(),
                lineNumber = index + 1,
                productId = product.id,
                sku = product.sku,
                productName = product.name,
                quantity = item.quantity,
                unitPrice = product.unitPrice,
                lineTotal = MoneyDto(lineTotal, product.unitPrice.currencyCode),
            )
        }
    }

    private fun replaceItems(orderId: UUID, items: List<OrderItemDto>) {
        jdbcClient.sql("delete from order_items where order_id = :orderId")
            .param("orderId", orderId)
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
                .param("id", item.id)
                .param("orderId", orderId)
                .param("productId", item.productId)
                .param("lineNumber", item.lineNumber)
                .param("sku", item.sku)
                .param("productName", item.productName)
                .param("quantity", item.quantity)
                .param("unitPrice", item.unitPrice.amount)
                .param("lineTotal", item.lineTotal.amount)
                .update()
        }
    }

    private fun insertEvent(
        orderId: UUID,
        eventType: OrderEventType,
        statusAfter: OrderStatus?,
        actor: String,
        reason: String?,
        occurredAt: OffsetDateTime,
        payload: JsonNode,
    ) {
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
            .param("orderId", orderId)
            .param("eventType", eventType.name)
            .param("statusAfter", statusAfter?.name)
            .param("actor", actor)
            .param("reason", reason)
            .param("payload", objectMapper.writeValueAsString(payload))
            .param("occurredAt", occurredAt)
            .update()
    }

    private fun updateOrderRecord(
        orderId: UUID,
        expectedVersion: Long,
        newStatus: OrderStatus,
        notes: String?,
        subtotal: BigDecimal,
        paidTotal: BigDecimal,
        refundedTotal: BigDecimal,
        trackingNumber: String?,
        paymentReference: String?,
        paidAt: OffsetDateTime?,
        shippedAt: OffsetDateTime?,
        completedAt: OffsetDateTime?,
        cancelledAt: OffsetDateTime?,
        updatedAt: OffsetDateTime,
    ) {
        val updatedRows = jdbcClient.sql(
            """
            update orders
            set status = :status,
                notes = :notes,
                subtotal = :subtotal,
                paid_total = :paidTotal,
                refunded_total = :refundedTotal,
                tracking_number = :trackingNumber,
                payment_reference = :paymentReference,
                paid_at = :paidAt,
                shipped_at = :shippedAt,
                completed_at = :completedAt,
                cancelled_at = :cancelledAt,
                updated_at = :updatedAt,
                version = version + 1
            where id = :id and version = :expectedVersion
            """.trimIndent(),
        )
            .param("status", newStatus.name)
            .param("notes", notes)
            .param("subtotal", subtotal.scaled())
            .param("paidTotal", paidTotal.scaled())
            .param("refundedTotal", refundedTotal.scaled())
            .param("trackingNumber", trackingNumber)
            .param("paymentReference", paymentReference)
            .param("paidAt", paidAt)
            .param("shippedAt", shippedAt)
            .param("completedAt", completedAt)
            .param("cancelledAt", cancelledAt)
            .param("updatedAt", updatedAt)
            .param("id", orderId)
            .param("expectedVersion", expectedVersion)
            .update()

        if (updatedRows != 1) {
            throw OrderLifecycleException(
                status = HttpStatus.CONFLICT,
                title = "Version conflict",
                message = "The order changed before this request was applied.",
            )
        }
    }

    private fun nextOrderNumber(): String {
        val maxExisting = jdbcClient.sql(
            """
            select coalesce(max(substring(order_number from '[0-9]+$')::bigint), 0)
            from orders
            """.trimIndent(),
        )
            .query(Long::class.java)
            .single()

        val nextValue = jdbcClient.sql("select nextval('order_number_seq')")
            .query(Long::class.java)
            .single()

        val resolvedValue = if (nextValue <= maxExisting) {
            jdbcClient.sql("select setval('order_number_seq', :value, true)")
                .param("value", maxExisting)
                .query(Long::class.java)
                .single() + 1
        } else {
            nextValue
        }

        return "ORD-${resolvedValue.toString().padStart(6, '0')}"
    }

    private fun mapOrderProjection(rs: ResultSet, rowNum: Int): OrderProjection {
        return OrderProjection(
            id = rs.getObject("id", UUID::class.java),
            orderNumber = rs.getString("order_number"),
            customerId = rs.getObject("customer_id", UUID::class.java),
            customerNumber = rs.getString("customer_number"),
            customerName = rs.getString("customer_name"),
            customerEmail = rs.getString("customer_email"),
            status = OrderStatus.valueOf(rs.getString("status")),
            notes = rs.getString("notes"),
            currencyCode = rs.getString("currency_code"),
            subtotal = rs.getBigDecimal("subtotal").scaled(),
            paidTotal = rs.getBigDecimal("paid_total").scaled(),
            refundedTotal = rs.getBigDecimal("refunded_total").scaled(),
            trackingNumber = rs.getString("tracking_number"),
            paymentReference = rs.getString("payment_reference"),
            paidAt = rs.getObject("paid_at", OffsetDateTime::class.java),
            shippedAt = rs.getObject("shipped_at", OffsetDateTime::class.java),
            completedAt = rs.getObject("completed_at", OffsetDateTime::class.java),
            cancelledAt = rs.getObject("cancelled_at", OffsetDateTime::class.java),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
            updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
            version = rs.getLong("version"),
        )
    }

    private fun toSummaryDto(projection: OrderProjection): OrderSummaryDto {
        return OrderSummaryDto(
            id = projection.id,
            orderNumber = projection.orderNumber,
            version = projection.version,
            status = projection.status,
            customer = projection.customer(),
            subtotal = MoneyDto(projection.subtotal, projection.currencyCode),
            paidTotal = MoneyDto(projection.paidTotal, projection.currencyCode),
            refundedTotal = MoneyDto(projection.refundedTotal, projection.currencyCode),
            outstandingTotal = MoneyDto(projection.balanceDue(), projection.currencyCode),
            createdAt = projection.createdAt,
            updatedAt = projection.updatedAt,
        )
    }

    private fun toDetailDto(
        projection: OrderProjection,
        items: List<OrderItemDto>,
        events: List<OrderEventDto>,
        refunds: List<OrderRefundDto>,
    ): OrderDetailDto {
        return OrderDetailDto(
            id = projection.id,
            orderNumber = projection.orderNumber,
            version = projection.version,
            status = projection.status,
            customer = projection.customer(),
            notes = projection.notes,
            subtotal = MoneyDto(projection.subtotal, projection.currencyCode),
            paidTotal = MoneyDto(projection.paidTotal, projection.currencyCode),
            refundedTotal = MoneyDto(projection.refundedTotal, projection.currencyCode),
            outstandingTotal = MoneyDto(projection.balanceDue(), projection.currencyCode),
            trackingNumber = projection.trackingNumber,
            paymentReference = projection.paymentReference,
            createdAt = projection.createdAt,
            updatedAt = projection.updatedAt,
            paidAt = projection.paidAt,
            shippedAt = projection.shippedAt,
            completedAt = projection.completedAt,
            cancelledAt = projection.cancelledAt,
            canEdit = projection.status in setOf(OrderStatus.PENDING, OrderStatus.PAID),
            canCancel = projection.status in setOf(OrderStatus.PENDING, OrderStatus.PAID),
            canRefund = projection.paidTotal > projection.refundedTotal,
            canShip = projection.status == OrderStatus.PAID,
            canComplete = projection.status == OrderStatus.SHIPPED,
            items = items,
            events = events,
            refunds = refunds,
        )
    }

    private fun OrderProjection.customer(): CustomerDto {
        return CustomerDto(
            id = customerId,
            customerNumber = customerNumber,
            name = customerName,
            email = customerEmail,
        )
    }

    private fun OrderProjection.balanceDue(): BigDecimal {
        val outstanding = subtotal.subtract(paidTotal)
        return if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal.ZERO.scaled()
        } else {
            outstanding.scaled()
        }
    }

    private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
