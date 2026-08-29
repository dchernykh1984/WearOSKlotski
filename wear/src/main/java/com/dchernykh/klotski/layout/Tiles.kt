package com.dchernykh.klotski.layout

import com.dchernykh.klotski.game.Block

// Where one block lands on the screen, and which block a finger landed on. Kept
// apart from the screen layout because this is the arithmetic that runs for every
// block of every frame, while that is worked out once per screen size.

/** The pixel size of a block that covers [cols] x [rows] cells. */
fun tileSize(
    cell: Int,
    gap: Int,
    cols: Int,
    rows: Int,
): Pair<Int, Int> = (cols * cell - 2 * gap) to (rows * cell - 2 * gap)

/**
 * The pixel box of a block standing at ([column], [row]) and covering [cols] x
 * [rows] cells, inset so it does not touch its neighbours.
 */
fun tileBox(
    board: BoardBox,
    column: Int,
    row: Int,
    cols: Int,
    rows: Int,
): Box {
    val (w, h) = tileSize(board.cell, board.gap, cols, rows)
    return Box(
        x = board.x + column * board.cell + board.gap,
        y = board.y + row * board.cell + board.gap,
        w = w,
        h = h,
    )
}

/** The box the selection ring is drawn on, just outside the block's own. */
fun selectionBox(
    tile: Box,
    margin: Int,
): Box = Box(x = tile.x - margin, y = tile.y - margin, w = tile.w + 2 * margin, h = tile.h + 2 * margin)

/** The cell under a touch, or null when the touch missed the board. */
fun cellAt(
    board: BoardBox,
    x: Int,
    y: Int,
): Pair<Int, Int>? {
    if ((x to y) !in Box(board.x, board.y, board.w, board.h)) return null
    return ((x - board.x) / board.cell) to ((y - board.y) / board.cell)
}

/** The pixel box of a block, wherever it currently stands. */
fun tileBox(
    board: BoardBox,
    block: Block,
): Box = tileBox(board, block.x, block.y, block.w, block.h)
