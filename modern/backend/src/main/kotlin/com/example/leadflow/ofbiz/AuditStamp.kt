package com.example.leadflow.ofbiz

import java.sql.Timestamp
import java.time.Instant

data class AuditStamp(
    val now: Timestamp = Timestamp.from(Instant.now()),
)
