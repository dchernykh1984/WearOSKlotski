package com.dchernykh.klotski.game

// Remembering the player's best game.
//
// Klotski is scored the other way round from most games - fewer moves is better -
// so "no record yet" cannot be zero, and every comparison here has to keep that
// straight.
//
// A result is a pair: the moves it took and how long it took. Moves decide the
// record; the clock only separates two games that took the same number of moves,
// because the puzzle is the point and the hurry is not. A game that was never
// timed still counts on moves, and loses a tie to one that was - otherwise an
// untimed game would sit at the top of a board forever, unbeatable except by
// playing it in fewer moves.

/** No result at all. Not a move count, because zero moves is not a game. */
const val NO_RECORD = 0

/** One finished game: how many moves it took, and how long. */
data class Result(
    val moves: Int = NO_RECORD,
    val time: Long = UNKNOWN,
) {
    val exists: Boolean get() = moves != NO_RECORD
}

/** A best score after a finished game, and whether that game is the one that set it. */
data class RecordOutcome(
    val best: Result,
    val isRecord: Boolean,
)

/**
 * A stored result, cleaned up. Anything missing or negative reads as "no record
 * yet", so a corrupt storage entry cannot make a record impossible to beat, and a
 * result with no moves has no clock either whatever was stored beside it.
 */
fun normalizeResult(result: Result?): Result {
    val moves = result?.moves ?: NO_RECORD
    if (moves <= 0) return Result()
    return Result(moves, normalizeElapsed(result?.time))
}

/**
 * Negative when [left] is the better game, positive when [right] is, zero when
 * there is nothing to choose between them.
 *
 * Fewer moves wins; on equal moves the shorter game wins; a game with no clock
 * loses that tie-break, and two games with no clock are simply equal.
 */
fun compareResults(
    left: Result?,
    right: Result?,
): Int {
    val a = normalizeResult(left)
    val b = normalizeResult(right)
    // A result that is not there at all is worse than any real one, and two of
    // them are equal. Booleans sort false before true, so the pair is compared
    // the other way round: the one that exists is the smaller, better result.
    if (!a.exists || !b.exists) return compareValues(b.exists, a.exists)
    if (a.moves != b.moves) return a.moves.compareTo(b.moves)
    return forOrdering(a.time).compareTo(forOrdering(b.time))
}

/**
 * A game with no clock loses the tie-break on equal moves, so for ordering alone
 * it counts as having taken longer than any game that was timed.
 */
private fun forOrdering(time: Long): Long = if (time == UNKNOWN) Long.MAX_VALUE else time

fun isBetter(
    candidate: Result?,
    current: Result?,
): Boolean = compareResults(candidate, current) < 0

/**
 * Fold a finished game into the record. A first finish always sets one; after that
 * only a better game does.
 */
fun updateBest(
    record: Result?,
    result: Result?,
): RecordOutcome {
    val current = normalizeResult(record)
    val candidate = normalizeResult(result)
    if (!candidate.exists) return RecordOutcome(current, isRecord = false)
    return if (isBetter(candidate, current)) {
        RecordOutcome(candidate, isRecord = true)
    } else {
        RecordOutcome(current, isRecord = false)
    }
}
