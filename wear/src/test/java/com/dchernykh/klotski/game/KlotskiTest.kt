package com.dchernykh.klotski.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** A board built by hand, so a test can start from a position it chooses. */
private fun boardOf(
    vararg art: String,
    par: Int = 1,
): Level {
    val (cols, rows, blocks) = parseArt(art.toList())
    return Level(id = 99, par = par, cols = cols, rows = rows, goal = GOAL, blocks = blocks)
}

class BlockAtTest {
    private val game = newGame(LEVELS.first())

    @Test
    fun `finds the block standing on a cell`() {
        // The first board is VHHV over VHHV, so the hero covers the middle two
        // columns of the top two rows.
        val hero = game.heroId
        assertEquals(hero, game.blockAt(1, 0))
        assertEquals(hero, game.blockAt(2, 1))
    }

    @Test
    fun `reports nothing for a free cell`() {
        assertNull(game.blockAt(0, 4))
    }

    @Test
    fun `reports nothing off the board`() {
        assertNull(game.blockAt(-1, 0))
        assertNull(game.blockAt(0, -1))
        assertNull(game.blockAt(BOARD_COLS, 0))
        assertNull(game.blockAt(0, BOARD_ROWS))
    }
}

class MoveTest {
    @Test
    fun `slides a block into a free cell and counts the move`() {
        val game = newGame(boardOf("S.", ".."))

        val next = game.moved(0, Direction.RIGHT)

        assertEquals(Block(Kind.SOLDIER, 1, 0), next.blocks[0])
        assertEquals(1, next.moves)
        assertEquals(listOf(Move(0, Direction.RIGHT)), next.history)
    }

    @Test
    fun `refuses to walk a block off the board`() {
        val game = newGame(boardOf("S.", ".."))

        assertSame(game, game.moved(0, Direction.UP))
        assertSame(game, game.moved(0, Direction.LEFT))
    }

    @Test
    fun `refuses to walk a block through another`() {
        val game = newGame(boardOf("SS", ".."))

        assertSame(game, game.moved(0, Direction.RIGHT))
    }

    @Test
    fun `refuses a block that is not there`() {
        val game = newGame(boardOf("S.", ".."))

        assertFalse(game.canMove(7, Direction.DOWN))
        assertSame(game, game.moved(7, Direction.DOWN))
    }

    @Test
    fun `moves a wide block only when every cell it covers is clear`() {
        // A guard lying across the top with a soldier under half of it: the guard
        // cannot come down until the soldier is out of the way, and then it can.
        val game = newGame(boardOf("GG", "S.", ".."))

        assertSame(game, game.moved(0, Direction.DOWN))

        val cleared = game.moved(1, Direction.DOWN).moved(0, Direction.DOWN)

        assertEquals(Block(Kind.GUARD, 0, 1), cleared.blocks[0])
    }
}

class UndoTest {
    @Test
    fun `takes back the last move and the counter with it`() {
        val game = newGame(boardOf("S.", ".."))
        val moved = game.moved(0, Direction.RIGHT)

        val back = moved.undone()

        assertEquals(game.blocks, back.blocks)
        assertEquals(0, back.moves)
        assertTrue(back.history.isEmpty())
    }

    @Test
    fun `does nothing at the start of a game`() {
        val game = newGame(boardOf("S.", ".."))

        assertSame(game, game.undone())
    }

    @Test
    fun `walks a whole game back to where it started`() {
        var game = newGame(LEVELS.first())
        val start = game
        val played = listOf(Direction.DOWN, Direction.DOWN, Direction.LEFT)
        for (direction in played) {
            for (id in game.blocks.indices) {
                val next = game.moved(id, direction)
                if (next !== game) {
                    game = next
                    break
                }
            }
        }
        assertTrue(game.moves > 0)

        repeat(game.moves) { game = game.undone() }

        assertEquals(start.blocks, game.blocks)
        assertEquals(0, game.moves)
    }
}

class RestartTest {
    @Test
    fun `puts the board back as it was dealt`() {
        val game = newGame(LEVELS.first()).moved(4, Direction.DOWN)

        val fresh = game.restarted()

        assertEquals(newGame(LEVELS.first()).blocks, fresh.blocks)
        assertEquals(0, fresh.moves)
        assertTrue(fresh.history.isEmpty())
    }
}

class SolvedTest {
    @Test
    fun `is not solved as it is dealt`() {
        for (level in LEVELS) {
            assertFalse("board ${level.id}", newGame(level).isSolved)
        }
    }

    @Test
    fun `is solved when the hero reaches the gate`() {
        val game = newGame(boardOf("....", "....", "....", ".HH.", ".HH."))

        assertTrue(game.isSolved)
    }
}

class ParseArtTest {
    @Test
    fun `reads a picture into blocks in board order`() {
        val (cols, rows, blocks) = parseArt(listOf("HH", "HH"))

        assertEquals(2, cols)
        assertEquals(2, rows)
        assertEquals(listOf(Block(Kind.HERO, 0, 0)), blocks)
    }

    @Test
    fun `refuses a picture with no cells`() {
        assertThrows { parseArt(emptyList()) }
        assertThrows { parseArt(listOf("")) }
    }

    @Test
    fun `refuses a ragged picture`() {
        assertThrows { parseArt(listOf("SS", "S")) }
    }

    @Test
    fun `refuses a block it does not know`() {
        assertThrows { parseArt(listOf("X.", "..")) }
    }

    @Test
    fun `refuses a block that hangs off the board`() {
        assertThrows { parseArt(listOf("VS")) }
    }

    @Test
    fun `refuses a block the picture only half draws`() {
        // A hero needs all four of its cells to carry the same letter.
        assertThrows { parseArt(listOf("HH", "H.")) }
    }

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected the board to be refused")
        } catch (_: IllegalArgumentException) {
            // The board was refused, which is the point.
        } catch (_: IllegalStateException) {
            // error() for an unknown letter; also a refusal.
        }
    }
}

class LevelListTest {
    @Test
    fun `numbers the boards from one, in order`() {
        assertEquals(LEVELS.indices.map { it + 1 }, LEVELS.map { it.id })
        assertEquals(1, FIRST_LEVEL)
    }

    @Test
    fun `deals every board on the classic frame`() {
        for (level in LEVELS) {
            assertEquals("board ${level.id}", BOARD_COLS, level.cols)
            assertEquals("board ${level.id}", BOARD_ROWS, level.rows)
            assertEquals(GOAL, level.goal)
            assertEquals("board ${level.id} has no hero", 1, level.blocks.count { it.kind == Kind.HERO })
        }
    }

    @Test
    fun `finds a board by its number`() {
        for (level in LEVELS) {
            assertEquals(level, levelById(level.id))
        }
    }

    @Test
    fun `falls back to the first board for a number it does not know`() {
        // A stored choice from an older version must not leave the game with
        // nothing to play.
        assertEquals(LEVELS.first(), levelById(0))
        assertEquals(LEVELS.first(), levelById(999))
        assertEquals(-1, levelIndex(999))
    }

    @Test
    fun `walks the ladder as a loop in both directions`() {
        assertEquals(LEVELS[1], nextLevel(LEVELS[0].id))
        assertEquals(LEVELS.first(), nextLevel(LEVELS.last().id))
        assertEquals(LEVELS[0], previousLevel(LEVELS[1].id))
        assertEquals(LEVELS.last(), previousLevel(LEVELS.first().id))
    }

    @Test
    fun `walks from an unknown number as though it were the first`() {
        // Forwards it lands ON the first board rather than past it: an unknown
        // number is treated as "before the ladder", so stepping forward reaches
        // the beginning of it - which is what the Zepp OS original does, and what
        // keeps a stored number from an older version from skipping a board.
        assertEquals(LEVELS.first(), nextLevel(999))
        assertEquals(LEVELS.last(), previousLevel(999))
    }
}
