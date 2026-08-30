package com.dchernykh.klotski.layout

import com.dchernykh.klotski.game.BOARD_COLS
import com.dchernykh.klotski.game.BOARD_ROWS
import com.dchernykh.klotski.game.GOAL
import com.dchernykh.klotski.game.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The round sizes Wear OS watches actually come in, small to large. */
private val SCREENS = listOf(384, 416, 454, 466, 480)

class ScreenLayoutTest {
    @Test
    fun `is exactly the design at the size it was designed for`() {
        val layout = screenLayout(DESIGN_SIZE)

        assertEquals(1f, scaleFor(DESIGN_SIZE), 0.0001f)
        assertEquals(DESIGN_CELL, layout.board.cell)
        assertEquals(DESIGN_TILE_GAP, layout.board.gap)
    }

    @Test
    fun `centres the tray on every screen`() {
        for (screen in SCREENS) {
            val board = screenLayout(screen).board
            assertEquals(board.w, BOARD_COLS * board.cell)
            assertEquals(board.h, BOARD_ROWS * board.cell)
            assertTrue(kotlin.math.abs(screen - board.x - board.w - board.x) <= 1)
            assertTrue(kotlin.math.abs(screen - board.y - board.h - board.y) <= 1)
        }
    }

    @Test
    fun `keeps the whole tray on the round screen`() {
        for (screen in SCREENS) {
            assertCornersOnScreen(screen, screenLayout(screen).tray, "the tray")
        }
    }

    @Test
    fun `never lets a cell shrink past the point of being playable`() {
        for (screen in SCREENS) {
            assertTrue(screenLayout(screen).board.cell >= MIN_CELL)
        }
    }

    @Test
    fun `puts the gate under the cell the hero has to reach`() {
        for (screen in SCREENS) {
            val layout = screenLayout(screen)
            val board = layout.board

            assertEquals(board.x + GOAL.x * board.cell, layout.gate.x)
            assertEquals(board.y + board.h, layout.gate.y)
            assertEquals(Kind.HERO.w * board.cell, layout.gate.w)
            // Right through the rim, so it reads as a way out rather than a stripe.
            assertEquals(layout.tray.y + layout.tray.h, layout.gate.y + layout.gate.h)
        }
    }

    @Test
    fun `puts the two round controls in the margins beside the board`() {
        for (screen in SCREENS) {
            val layout = screenLayout(screen)

            assertTrue(layout.undo.x + layout.undo.w <= layout.tray.x)
            assertTrue(layout.menu.x >= layout.tray.x + layout.tray.w)
            assertEquals(layout.undo.y, layout.menu.y)
            assertCornersOnScreen(screen, layout.undo, "undo")
            assertCornersOnScreen(screen, layout.menu, "menu")
        }
    }

    @Test
    fun `keeps the counter above the board and the restart button below it`() {
        for (screen in SCREENS) {
            val layout = screenLayout(screen)

            assertTrue(layout.counter.y + layout.counter.h <= layout.tray.y)
            assertTrue(layout.restart.y >= layout.tray.y + layout.tray.h)
            assertCornersOnScreen(screen, layout.counter, "the counter")
            assertCornersOnScreen(screen, layout.restart, "restart")
        }
    }

    @Test
    fun `keeps every line of the records screen on the round face`() {
        for (screen in SCREENS) {
            val records = screenLayout(screen).records
            val lines = listOf(records.above, records.title, records.below, records.back) + records.rows

            for (line in lines) {
                assertCornersOnScreen(screen, line, "a records line")
            }
        }
    }

    @Test
    fun `stacks the records screen from the board above to the way out`() {
        val records = screenLayout(DESIGN_SIZE).records
        val tops = listOf(records.above, records.title) + records.rows + listOf(records.below, records.back)

        assertEquals(tops.map { it.y }.sorted(), tops.map { it.y })
        assertEquals(RECORD_LINES, records.rows.size)
    }

    @Test
    fun `never shrinks type below the point of being readable`() {
        for (screen in SCREENS) {
            val text = screenLayout(screen).text
            for (size in listOf(text.title, text.row, text.small, text.hint, text.button)) {
                assertTrue("type shrank to $size on a $screen screen", size >= MIN_TEXT)
            }
        }
    }
}

class TilesTest {
    private val layout = screenLayout(DESIGN_SIZE)

    @Test
    fun `insets a tile inside its cells on every side`() {
        val box = tileBox(layout.board, 0, 0, 1, 1)

        assertEquals(layout.board.x + layout.board.gap, box.x)
        assertEquals(layout.board.cell - 2 * layout.board.gap, box.w)
    }

    @Test
    fun `makes a wide tile span its cells`() {
        val hero = tileBox(layout.board, 1, 0, 2, 2)

        assertEquals(2 * layout.board.cell - 2 * layout.board.gap, hero.w)
        assertEquals(2 * layout.board.cell - 2 * layout.board.gap, hero.h)
    }

    @Test
    fun `leaves neighbouring tiles a gap between them`() {
        val left = tileBox(layout.board, 0, 0, 1, 1)
        val right = tileBox(layout.board, 1, 0, 1, 1)

        assertTrue(left.x + left.w < right.x)
    }

    @Test
    fun `keeps every tile inside the board`() {
        val last = tileBox(layout.board, BOARD_COLS - 1, BOARD_ROWS - 1, 1, 1)

        assertTrue(last.x + last.w <= layout.board.x + layout.board.w)
        assertTrue(last.y + last.h <= layout.board.y + layout.board.h)
    }

    @Test
    fun `draws the selection ring outside the block it marks`() {
        val tile = tileBox(layout.board, 0, 0, 1, 1)
        val ring = selectionBox(tile, layout.selection.margin)

        assertTrue(ring.x < tile.x)
        assertTrue(ring.y < tile.y)
        assertEquals(tile.w + 2 * layout.selection.margin, ring.w)
    }

    @Test
    fun `turns a touch into the cell under it`() {
        val board = layout.board

        assertEquals(0 to 0, cellAt(board, board.x, board.y))
        assertEquals(1 to 2, cellAt(board, board.x + board.cell, board.y + 2 * board.cell))
        assertEquals(
            (BOARD_COLS - 1) to (BOARD_ROWS - 1),
            cellAt(board, board.x + board.w - 1, board.y + board.h - 1),
        )
    }

    @Test
    fun `reports nothing for a touch that missed the board`() {
        val board = layout.board

        assertNull(cellAt(board, board.x - 1, board.y))
        assertNull(cellAt(board, board.x, board.y - 1))
        assertNull(cellAt(board, board.x + board.w, board.y))
        assertNull(cellAt(board, board.x, board.y + board.h))
    }
}
