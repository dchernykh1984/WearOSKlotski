package com.dchernykh.klotski.game

// The rules of Klotski (Huarong Pass), with nothing Android in them, so every
// rule is exercised by a unit test and the screen only has to draw what these
// decide.
//
// The board is a grid holding rectangular blocks. A block slides one cell at a
// time into free cells; it never rotates and never leaves the board. The puzzle
// is solved when the single 2x2 hero reaches the cell in front of the exit.
//
// Every move is one cell, so a player who pushes a block across two cells has
// spent two moves - which is what the counter shows and what a board's par
// counts.
//
// A position is an immutable value and a move is a function from one to the next,
// which is what lets a test start from any board rather than only from one it
// could reach by playing.

/**
 * What a block is, which decides its footprint, its portrait, and the letter it is
 * written as in a board's picture.
 *
 * The letter lives on the enum rather than in a table beside it, so that anything
 * needing to tell two kinds apart agrees with the art by construction. Guard and
 * general are the case that matters: they are the same word to two decimal places
 * and different shapes, and a mark that collapsed them would quietly merge
 * positions that are not the same position at all.
 */
enum class Kind(
    val w: Int,
    val h: Int,
    val mark: Char,
) {
    HERO(2, 2, 'H'),
    GUARD(2, 1, 'G'),
    GENERAL(1, 2, 'V'),
    SOLDIER(1, 1, 'S'),
}

/** A block standing with its top left corner at ([x], [y]). */
data class Block(
    val kind: Kind,
    val x: Int,
    val y: Int,
) {
    val w: Int get() = kind.w
    val h: Int get() = kind.h

    fun covers(
        column: Int,
        row: Int,
    ): Boolean = column >= x && column < x + w && row >= y && row < y + h
}

/** Declared clockwise, like the compass, so the tests read the way a screen does. */
enum class Direction(
    val dx: Int,
    val dy: Int,
) {
    UP(0, -1),
    RIGHT(1, 0),
    DOWN(0, 1),
    LEFT(-1, 0),
}

/** One move, kept so it can be taken back. */
data class Move(
    val id: Int,
    val direction: Direction,
)

/**
 * A game in progress. [blocks] is indexed by block id, and a block's id never
 * changes, so [history] can name one without holding on to it.
 */
data class GameState(
    val level: Level,
    val blocks: List<Block>,
    val moves: Int = 0,
    val history: List<Move> = emptyList(),
) {
    val cols: Int get() = level.cols
    val rows: Int get() = level.rows

    val heroId: Int get() = blocks.indexOfFirst { it.kind == Kind.HERO }

    /** The hero is out when it stands on the cell in front of the gate. */
    val isSolved: Boolean
        get() = blocks.getOrNull(heroId)?.let { it.x == level.goal.x && it.y == level.goal.y } == true
}

/** The board as it is dealt. */
fun newGame(level: Level): GameState = GameState(level = level, blocks = level.blocks)

/** The same board again, from the beginning. */
fun GameState.restarted(): GameState = newGame(level)

/**
 * The block covering a cell, as its id, or null when the cell is free or off the
 * board. This is how a tap that arrives as a pair of screen coordinates becomes
 * the block that was under the finger.
 */
fun GameState.blockAt(
    column: Int,
    row: Int,
): Int? {
    if (!isOnBoard(column, row)) return null
    val id = blocks.indexOfFirst { it.covers(column, row) }
    return if (id == -1) null else id
}

private fun GameState.isOnBoard(
    column: Int,
    row: Int,
): Boolean = column >= 0 && row >= 0 && column < cols && row < rows

/**
 * Whether a block's whole footprint would land on the board and on cells nothing
 * but [id] itself is standing on.
 */
private fun GameState.fits(
    id: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
): Boolean {
    if (!isOnBoard(x, y) || !isOnBoard(x + w - 1, y + h - 1)) return false
    for (dy in 0 until h) {
        for (dx in 0 until w) {
            val occupant = blockAt(x + dx, y + dy)
            if (occupant != null && occupant != id) return false
        }
    }
    return true
}

/**
 * Whether a block can slide one cell: every cell it would move into has to be on
 * the board and either free or already covered by the block itself.
 */
fun GameState.canMove(
    id: Int,
    direction: Direction,
): Boolean {
    val block = blocks.getOrNull(id) ?: return false
    return fits(id, block.x + direction.dx, block.y + direction.dy, block.w, block.h)
}

/**
 * The position after sliding a block one cell, or this one unchanged when the
 * rules refuse the move.
 */
fun GameState.moved(
    id: Int,
    direction: Direction,
): GameState {
    if (!canMove(id, direction)) return this
    val block = blocks[id]
    val slid = blocks.toMutableList()
    slid[id] = block.copy(x = block.x + direction.dx, y = block.y + direction.dy)
    return copy(blocks = slid, moves = moves + 1, history = history + Move(id, direction))
}

/**
 * The position one move ago. The counter goes back with it: undo is a way to
 * think, not a way to score.
 */
fun GameState.undone(): GameState {
    val last = history.lastOrNull() ?: return this
    val block = blocks[last.id]
    val back = blocks.toMutableList()
    back[last.id] = block.copy(x = block.x - last.direction.dx, y = block.y - last.direction.dy)
    return copy(blocks = back, moves = moves - 1, history = history.dropLast(1))
}
