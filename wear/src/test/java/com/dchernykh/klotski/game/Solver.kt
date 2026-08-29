package com.dchernykh.klotski.game

// A breadth-first search for the shortest game, used by the tests to check what
// the app claims: that each board's par really is its minimum, and - in the view
// model's tests - to play a board through to its solution without a hand-written
// list of moves for each one.
//
// It lives in the test source set on purpose. The app never solves a board; the
// player does.

/**
 * Two positions are the same when the same *kinds* of block stand in the same
 * cells. Two soldiers are interchangeable, and telling them apart would multiply
 * the search by thousands of positions that are the same puzzle.
 *
 * The mark comes from [Kind], which is also what the board pictures are written
 * in, so guard and general - the same word to two decimal places, and different
 * shapes - can never collapse into each other here.
 */
fun signature(state: GameState): String {
    val grid = CharArray(state.cols * state.rows) { '.' }
    for (block in state.blocks) {
        for (dy in 0 until block.h) {
            for (dx in 0 until block.w) {
                grid[(block.y + dy) * state.cols + block.x + dx] = block.kind.mark
            }
        }
    }
    return String(grid)
}

/** Every position one move away, with the move that reaches it. */
private fun successors(state: GameState): List<Pair<GameState, Move>> =
    state.blocks.indices.flatMap { id ->
        Direction.entries.mapNotNull { direction ->
            val moved = state.moved(id, direction)
            if (moved === state) null else moved to Move(id, direction)
        }
    }

/**
 * Walk one position's successors. Returns the solution as soon as one is reached,
 * and otherwise adds what is worth exploring to [next].
 */
private fun expand(
    state: GameState,
    path: List<Move>,
    seen: MutableSet<String>,
    next: MutableList<Pair<GameState, List<Move>>>,
): List<Move>? {
    for ((moved, move) in successors(state)) {
        if (seen.add(signature(moved))) {
            val walked = path + move
            if (moved.isSolved) return walked
            next.add(moved to walked)
        }
    }
    return null
}

/** The shortest game from this position, or null when there is no way out at all. */
fun shortestSolution(state: GameState): List<Move>? {
    if (state.isSolved) return emptyList()
    val seen = hashSetOf(signature(state))
    var frontier = listOf(state to emptyList<Move>())

    while (frontier.isNotEmpty()) {
        val next = ArrayList<Pair<GameState, List<Move>>>()
        for ((position, path) in frontier) {
            expand(position, path, seen, next)?.let { return it }
        }
        frontier = next
    }
    return null
}
