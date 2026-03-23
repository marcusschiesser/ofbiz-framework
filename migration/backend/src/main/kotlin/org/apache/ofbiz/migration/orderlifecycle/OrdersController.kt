package org.apache.ofbiz.migration.orderlifecycle

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class OrdersController(
    private val orderService: OrderService,
) {

    @GetMapping("/orders")
    @Operation(summary = "List orders")
    fun listOrders(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) status: OrderStatus?,
    ): OrderListResponse {
        return OrderListResponse(orderService.listOrders(query, status))
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order detail")
    fun getOrder(@PathVariable orderId: UUID): OrderDetailDto = orderService.getOrder(orderId)

    @PostMapping("/orders")
    @Operation(summary = "Create order")
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): OrderDetailDto = orderService.createOrder(request)

    @PatchMapping("/orders/{orderId}")
    @Operation(summary = "Update order")
    fun updateOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: UpdateOrderRequest,
    ): OrderDetailDto = orderService.updateOrder(orderId, request)

    @PostMapping("/orders/{orderId}/actions/pay")
    @Operation(summary = "Mark order as paid")
    fun payOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: OrderActionRequest,
    ): OrderDetailDto = orderService.markPaid(orderId, request)

    @PostMapping("/orders/{orderId}/actions/ship")
    @Operation(summary = "Mark order as shipped")
    fun shipOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: OrderActionRequest,
    ): OrderDetailDto = orderService.markShipped(orderId, request)

    @PostMapping("/orders/{orderId}/actions/complete")
    @Operation(summary = "Mark order as completed")
    fun completeOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: OrderActionRequest,
    ): OrderDetailDto = orderService.markCompleted(orderId, request)

    @PostMapping("/orders/{orderId}/actions/cancel")
    @Operation(summary = "Cancel order")
    fun cancelOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: OrderActionRequest,
    ): OrderDetailDto = orderService.cancelOrder(orderId, request)

    @PostMapping("/orders/{orderId}/actions/refund")
    @Operation(summary = "Refund order")
    fun refundOrder(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: RefundOrderRequest,
    ): OrderDetailDto = orderService.refundOrder(orderId, request)

    @GetMapping("/reference-data")
    @Operation(summary = "Load customers and products for the order create flow")
    fun getReferenceData(): ReferenceDataResponse = orderService.getReferenceData()
}
