package com.example.leadflow.ofbiz

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class AuditStamp(
    val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
) {
    val bigNow: OffsetDateTime = now
    val zero: BigDecimal = BigDecimal.ZERO
}
