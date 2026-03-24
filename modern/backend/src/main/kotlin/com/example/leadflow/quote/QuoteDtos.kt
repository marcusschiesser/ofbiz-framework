package com.example.leadflow.quote

import java.math.BigDecimal

data class QuoteItemDetail(
    val seqId: String,
    val productId: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val sourceRequestItemSeqId: String?,
    val comments: String?,
)

data class QuoteDetail(
    val quoteId: String,
    val quoteTypeId: String,
    val statusId: String,
    val partyId: String,
    val quoteName: String?,
    val productStoreId: String?,
    val salesChannelEnumId: String?,
    val items: List<QuoteItemDetail>,
    val orderIds: List<String>,
)
