package com.mart.villagemart.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecimalValueTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun encodesExactDecimalAsJsonNumber() {
        val encoded = json.encodeToString(DecimalValue("123456789012.3400"))
        assertEquals("123456789012.3400", encoded)
        assertTrue(encoded.toBigDecimal().compareTo("123456789012.34".toBigDecimal()) == 0)
    }

    @Test
    fun decodesJsonNumberAndStringWithoutFloatingPointConversion() {
        assertEquals(DecimalValue("0.001"), json.decodeFromString(DecimalValue.serializer(), "0.001"))
        assertEquals(DecimalValue("987654321.09"), json.decodeFromString(DecimalValue.serializer(), "\"987654321.09\""))
    }
}
