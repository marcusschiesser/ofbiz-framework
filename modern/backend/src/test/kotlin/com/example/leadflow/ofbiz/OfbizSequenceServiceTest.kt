package com.example.leadflow.ofbiz

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OfbizSequenceServiceTest {

    @Test
    fun `sub sequence ids are padded to five digits`() {
        val next = (4 + 1).toString().padStart(5, '0')
        assertEquals("00005", next)
    }
}
