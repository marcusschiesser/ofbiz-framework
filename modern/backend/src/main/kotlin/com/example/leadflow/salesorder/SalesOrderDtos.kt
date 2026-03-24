package com.example.leadflow.salesorder

import java.math.BigDecimal

data class SalesOrderItemDetail(
    val seqId: String,
    val productId: String?,
    val description: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val statusId: String,
)

data class SalesOrderDetail(
    val orderId: String,
    val statusId: String,
    val orderTypeId: String,
    val partyId: String,
    val quoteId: String?,
    val productStoreId: String?,
    val webSiteId: String?,
    val grandTotal: BigDecimal,
    val items: List<SalesOrderItemDetail>,
)
