package com.dchernykh.klotski.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizeResultTest {
    @Test
    fun `keeps a real result`() {
        assertEquals(Result(42, 9_000), normalizeResult(Result(42, 9_000)))
    }

    @Test
    fun `reads nothing stored as no record yet`() {
        assertFalse(normalizeResult(null).exists)
        assertFalse(normalizeResult(Result(0, 9_000)).exists)
        assertFalse(normalizeResult(Result(-3, 9_000)).exists)
    }

    @Test
    fun `drops the clock of a result that has no moves`() {
        // Zero moves is not a game, so whatever was stored beside it is not a time.
        assertEquals(Result(NO_RECORD, UNKNOWN), normalizeResult(Result(0, 9_000)))
    }
}

class CompareResultsTest {
    @Test
    fun `puts fewer moves first`() {
        assertTrue(compareResults(Result(40, 9_000), Result(50, 1_000)) < 0)
        assertTrue(compareResults(Result(50, 1_000), Result(40, 9_000)) > 0)
    }

    @Test
    fun `separates equal games by the clock`() {
        assertTrue(compareResults(Result(40, 1_000), Result(40, 9_000)) < 0)
        assertEquals(0, compareResults(Result(40, 1_000), Result(40, 1_000)))
    }

    @Test
    fun `makes an untimed game lose the tie-break`() {
        // Otherwise an untimed game would sit at the top of a board forever,
        // unbeatable except by playing it in fewer moves.
        assertTrue(compareResults(Result(40, 1_000), Result(40, UNKNOWN)) < 0)
        assertTrue(compareResults(Result(40, UNKNOWN), Result(40, 1_000)) > 0)
        assertEquals(0, compareResults(Result(40, UNKNOWN), Result(40, UNKNOWN)))
    }

    @Test
    fun `puts any real result ahead of none at all`() {
        assertTrue(compareResults(Result(500, UNKNOWN), null) < 0)
        assertTrue(compareResults(null, Result(500, UNKNOWN)) > 0)
        assertEquals(0, compareResults(null, null))
    }

    @Test
    fun `agrees with itself about which is better`() {
        assertTrue(isBetter(Result(10, 1_000), Result(20, 1_000)))
        assertFalse(isBetter(Result(20, 1_000), Result(10, 1_000)))
        assertFalse(isBetter(Result(10, 1_000), Result(10, 1_000)))
    }
}

class UpdateBestTest {
    @Test
    fun `takes the first finish as a record`() {
        val outcome = updateBest(null, Result(80, 60_000))

        assertEquals(Result(80, 60_000), outcome.best)
        assertTrue(outcome.isRecord)
    }

    @Test
    fun `records a shorter game`() {
        val outcome = updateBest(Result(80, 60_000), Result(70, 90_000))

        assertEquals(Result(70, 90_000), outcome.best)
        assertTrue(outcome.isRecord)
    }

    @Test
    fun `keeps the record when the game was longer`() {
        val outcome = updateBest(Result(70, 10_000), Result(80, 1_000))

        assertEquals(Result(70, 10_000), outcome.best)
        assertFalse(outcome.isRecord)
    }

    @Test
    fun `records a faster game of the same length`() {
        val outcome = updateBest(Result(70, 60_000), Result(70, 30_000))

        assertEquals(Result(70, 30_000), outcome.best)
        assertTrue(outcome.isRecord)
    }

    @Test
    fun `ignores a result that is not a finished game`() {
        val outcome = updateBest(Result(70, 10_000), Result(NO_RECORD, 1_000))

        assertEquals(Result(70, 10_000), outcome.best)
        assertFalse(outcome.isRecord)
    }
}
