package com.example.albionmarketv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class TradeCalculatorTest {

    @Test
    fun testNormalizeCityName() {
        assertEquals("fortsterling", TradeCalculator.normalizeCityName("Fort Sterling"))
        assertEquals("blackmarket", TradeCalculator.normalizeCityName("Black Market"))
        assertEquals("caerleon", TradeCalculator.normalizeCityName("Caerleon"))
        assertEquals("bridgewatch", TradeCalculator.normalizeCityName("3005"))
    }

    @Test
    fun testCitiesMatch() {
        assertTrue(TradeCalculator.citiesMatch("Fort Sterling", "fortsterling"))
        assertTrue(TradeCalculator.citiesMatch("Black Market", "Schwarzmarkt"))
        assertFalse(TradeCalculator.citiesMatch("Caerleon", "Lymhurst"))
    }

    @Test
    fun testCalculateTotalCapacityKg() {
        val capacity = TradeCalculator.calculateTotalCapacityKg("Ochs T4", "Tasche T5 (+220 kg)", "Transport-Schuhe T4 (+80 kg)")
        assertEquals(1600.0, capacity, 0.01)
    }
}
