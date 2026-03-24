package com.example.leadflow.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("leadflow.ofbiz")
data class OfbizProperties(
    val baseUrl: String,
    val createdByUserLoginId: String,
    val internalOrganizationPartyId: String,
    val leadOwnerPartyId: String,
    val productStoreId: String,
    val webSiteId: String,
    val salesChannelEnumId: String,
    val currencyUomId: String,
    val defaultRequestTypeId: String,
    val defaultRequestStatusId: String,
    val defaultQuoteTypeId: String,
    val defaultQuoteStatusId: String,
    val defaultOrderTypeId: String,
    val defaultOrderStatusId: String,
    val defaultOrderItemStatusId: String,
)
