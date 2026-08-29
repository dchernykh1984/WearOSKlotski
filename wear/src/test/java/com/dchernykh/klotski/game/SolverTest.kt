package com.dchernykh.klotski.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every board's par, recomputed.
 *
 * `par` is the shortest possible game in single-cell moves, and it is a claim the
 * records screen makes to the player. Searching for it proves the claim, so a
 * mistyped board or a wrong number fails the build rather than shipping as a
 * target nobody can reach - or one anybody can beat.
 */
class SolverTest {
    @Test
    fun `every board is solvable in exactly the moves it claims`() {
        for (level in LEVELS) {
            val solution = shortestSolution(newGame(level))
            assertNotNull("board ${level.id} cannot be solved at all", solution)
            assertEquals("board ${level.id}", level.par, solution!!.size)
        }
    }

    @Test
    fun `the solution it finds really does solve the board`() {
        // Replaying it through the same rules the game uses is what makes the
        // number above worth anything.
        for (level in LEVELS) {
            var state = newGame(level)
            for ((id, direction) in shortestSolution(state)!!) {
                state = state.moved(id, direction)
            }
            assertTrue("board ${level.id}", state.isSolved)
            assertEquals(level.par, state.moves)
        }
    }

    @Test
    fun `the ladder gets harder as it goes`() {
        val pars = LEVELS.map { it.par }
        assertEquals(pars.sorted(), pars)
        assertEquals("boards should not tie on difficulty", pars.size, pars.toSet().size)
    }

    @Test
    fun `ends with the classic Huarong Pass`() {
        // The real puzzle cannot be solved in fewer than 116 single-cell moves,
        // which is the number this whole test exists to keep honest.
        assertEquals(116, LEVELS.last().par)
    }

    @Test
    fun `finds nothing to do on a board that is already solved`() {
        val solved = newGame(LEVELS.first()).copy(blocks = listOf(Block(Kind.HERO, GOAL.x, GOAL.y)))

        assertEquals(emptyList<Move>(), shortestSolution(solved))
    }
}
