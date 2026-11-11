package com.bawabat.alorood

import com.bawabat.alorood.data.Demo
import com.bawabat.alorood.data.FloorState
import com.bawabat.alorood.util.Validation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    @Test fun arabicDigitsAreNormalized() {
        assertEquals("0501234567", Validation.normalizeDigits("٠٥٠١٢٣٤٥٦٧"))
        assertEquals("123", Validation.normalizeDigits("۱۲۳"))
    }

    @Test fun phoneValidation() {
        assertTrue(Validation.isValidPhone("557545350"))
        assertTrue(Validation.isValidPhone("٠٥٥٧٥٤٥٣٥٠"))
        assertFalse(Validation.isValidPhone("12345"))
        assertFalse(Validation.isValidPhone(""))
    }

    @Test fun fullPhoneTrimsLeadingZero() {
        assertEquals("+966557545350", Validation.fullPhone("+966", "0557545350"))
        assertEquals("+20100123456", Validation.fullPhone("+20", "100123456"))
    }

    @Test fun passwordRules() {
        assertEquals("short", Validation.passwordError("ab1"))
        assertEquals("letter", Validation.passwordError("12345678"))
        assertEquals("digit", Validation.passwordError("abcdefgh"))
        assertNull(Validation.passwordError("abcd1234"))
    }

    @Test fun offerRule() {
        assertTrue(Validation.isOfferValid(500_000, 500_000))
        assertTrue(Validation.isOfferValid(550_000, 500_000))
        assertFalse(Validation.isOfferValid(499_999, 500_000))
        assertFalse(Validation.isOfferValid(0, 0))
    }

    @Test fun priceFormatting() {
        assertEquals("1,000,000", Validation.formatPrice(1_000_000))
        assertEquals("550,000", Validation.formatPrice(550_000))
        assertEquals("999", Validation.formatPrice(999))
        assertEquals("0", Validation.formatPrice(0))
    }

    @Test fun amountParsing() {
        assertEquals(550_000L, Validation.parseAmount("٥٥٠٠٠٠"))
        assertEquals(55L, Validation.parseAmount("5a5"))
        assertEquals(0L, Validation.parseAmount(""))
    }

    @Test fun holdDaysCountdown() {
        val day = 86_400_000L
        assertEquals(3, Validation.daysLeft(0, 0))
        assertEquals(2, Validation.daysLeft(0, day))
        assertEquals(1, Validation.daysLeft(0, (2.5 * day).toLong()))
        assertEquals(0, Validation.daysLeft(0, 3 * day))
        assertEquals(0, Validation.daysLeft(0, 10 * day))
    }

    @Test fun sha256KnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Validation.sha256("abc"))
    }

    @Test fun distanceIsZeroForSamePoint() {
        assertEquals(0.0, Validation.distanceKm(21.5, 39.2, 21.5, 39.2), 0.0001)
        assertTrue(Validation.distanceKm(21.5, 39.2, 21.6, 39.2) in 10.0..12.0)
    }
}

class DemoDataTest {
    @Test fun idsAreUnique() {
        val ids = Demo.properties.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.size >= 10)
    }

    @Test fun everyListingHasAnAvailableFloorAndValidMinOffer() {
        for (p in Demo.properties) {
            assertTrue(p.id, p.floors.any { it.state == FloorState.AVAILABLE })
            assertTrue(p.id, p.minOffer in 1..p.price)
            assertTrue(p.id, p.price > 0 && p.area > 0)
        }
    }

    @Test fun typeKeysAreUnique() {
        val keys = Demo.types.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test fun typeMatchingWorks() {
        val p = Demo.properties.first { it.kind == "apt" && it.rooms == 4 }
        assertTrue(Demo.types.first { it.key == "apt4" }.matches(p))
        assertFalse(Demo.types.first { it.key == "apt3" }.matches(p))
    }
}
