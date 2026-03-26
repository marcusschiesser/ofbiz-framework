package com.example.leadflow.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "leadflow.ofbiz")
data class OfbizProperties(
    val baseUrl: String,
    val createdByUserLoginId: String,
    val leadOwnerPartyId: String,
    val currencyUomId: String,
    val defaultRequestTypeId: String,
    val defaultRequestStatusId: String,
)
