package com.dchernykh.klotski.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElapsedBetweenTest {
    @Test
    fun `measures a game from its start to its finish`() {
        assertEquals(4_000L, elapsedBetween(1_000, 5_000))
    }

    @Test
    fun `reports nothing when a stamp is missing`() {
        assertEquals(UNKNOWN, elapsedBetween(0, 5_000))
        assertEquals(UNKNOWN, elapsedBetween(1_000, 0))
    }

    @Test
    fun `reports nothing when the watch's clock moved underneath us`() {
        // A negative duration would be worse than none.
        assertEquals(UNKNOWN, elapsedBetween(5_000, 1_000))
        assertEquals(UNKNOWN, elapsedBetween(5_000, 5_000))
    }

    @Test
    fun `stops growing at the point a game stopped being one`() {
        assertEquals(MAX_ELAPSED, elapsedBetween(1, Long.MAX_VALUE / 2))
    }
}

class NormalizeElapsedTest {
    @Test
    fun `keeps a real duration`() {
        assertEquals(4_000L, normalizeElapsed(4_000))
        assertTrue(isTimed(4_000))
    }

    @Test
    fun `reads nothing stored as an untimed game`() {
        assertEquals(UNKNOWN, normalizeElapsed(null))
        assertEquals(UNKNOWN, normalizeElapsed(0))
        assertEquals(UNKNOWN, normalizeElapsed(-5))
        assertFalse(isTimed(UNKNOWN))
    }

    @Test
    fun `caps a duration no game could have taken`() {
        assertEquals(MAX_ELAPSED, normalizeElapsed(Long.MAX_VALUE))
    }
}

class FormatElapsedTest {
    @Test
    fun `writes minutes and seconds`() {
        assertEquals("4:21", formatElapsed(4 * 60_000 + 21_000))
        assertEquals("0:07", formatElapsed(7_000))
    }

    @Test
    fun `pads the seconds so the number does not jitter as it counts`() {
        assertEquals("1:05", formatElapsed(65_000))
    }

    @Test
    fun `leaves the leading field unpadded, so it does not read as a time of day`() {
        assertEquals("1:04:21", formatElapsed(3_600_000 + 4 * 60_000 + 21_000))
    }

    @Test
    fun `writes the placeholder for a game that was never timed`() {
        assertEquals("-", formatElapsed(UNKNOWN))
        assertEquals("n/a", formatElapsed(UNKNOWN, "n/a"))
    }
}
