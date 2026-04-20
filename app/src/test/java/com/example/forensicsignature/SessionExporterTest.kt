package com.example.forensicsignature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExporterTest {

    @Test
    fun sha256_isDeterministic() {
        val exporter = SessionExporter()
        val first = exporter.sha256Hex("abc".toByteArray())
        val second = exporter.sha256Hex("abc".toByteArray())

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.matches(Regex("[0-9a-f]+")))
    }
}
