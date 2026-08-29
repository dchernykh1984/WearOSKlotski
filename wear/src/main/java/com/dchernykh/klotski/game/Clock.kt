package com.dchernykh.klotski.game

// How long a game took, and how to write that on a watch face. Pure arithmetic
// with no clock of its own: the screen reads the time and hands the two stamps in
// here, which is what makes any of it testable.
//
// A game is timed from the moment the board opens to the moment the hero is out,
// and nothing in between stops the clock. That is deliberate: a rule like "the
// clock pauses in the menu" only invites the player to think in the menu, and
// pausing it while the screen sleeps would need the watch to agree with itself
// about when a game is really being played. Finish minus start is a rule anyone
// can hold in their head.

/** A game that was never timed. */
const val UNKNOWN = 0L

private const val SECOND = 1000L
private const val MINUTE = 60 * SECOND
private const val HOUR = 60 * MINUTE

/**
 * A game longer than this is not a game any more - a watch left in a drawer, or a
 * clock that jumped. It is still recorded, but it stops growing here so the record
 * screen never has to draw a number that does not fit.
 */
const val MAX_ELAPSED = 100 * HOUR - SECOND

/**
 * The milliseconds between two readings of the clock, or [UNKNOWN] when there is
 * nothing sensible to report.
 *
 * A missing stamp means the game was not timed at all; a finish before its start
 * means the watch's clock moved underneath us, and a negative duration would be
 * worse than none.
 */
fun elapsedBetween(
    startedAt: Long,
    finishedAt: Long,
): Long {
    if (startedAt <= 0 || finishedAt <= 0) return UNKNOWN
    val elapsed = finishedAt - startedAt
    if (elapsed <= 0) return UNKNOWN
    return minOf(elapsed, MAX_ELAPSED)
}

/**
 * A stored duration turned back into milliseconds. Anything missing or negative
 * reads as "not timed", so a corrupt entry cannot make a record impossible to
 * beat.
 */
fun normalizeElapsed(value: Long?): Long {
    val elapsed = value ?: return UNKNOWN
    if (elapsed <= 0) return UNKNOWN
    return minOf(elapsed, MAX_ELAPSED)
}

fun isTimed(elapsed: Long): Boolean = normalizeElapsed(elapsed) != UNKNOWN

/**
 * A duration as a watch would write it: "4:21" for a few minutes, "1:04:21" once
 * it runs past the hour.
 *
 * Seconds are always two digits so the number does not jitter as it counts; the
 * leading field never is, because "04:21" reads like a time of day rather than a
 * length. An untimed game gets the placeholder the screen uses everywhere else.
 */
fun formatElapsed(
    elapsed: Long,
    placeholder: String = "-",
): String {
    val total = normalizeElapsed(elapsed)
    if (total == UNKNOWN) return placeholder
    val seconds = (total / SECOND) % 60
    val minutes = (total / MINUTE) % 60
    val hours = total / HOUR
    return if (hours > 0) {
        "$hours:${pad(minutes)}:${pad(seconds)}"
    } else {
        "$minutes:${pad(seconds)}"
    }
}

private fun pad(value: Long): String = if (value < 10) "0$value" else value.toString()
