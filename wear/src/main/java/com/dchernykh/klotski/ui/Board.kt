package com.dchernykh.klotski.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.dchernykh.klotski.game.GameState
import com.dchernykh.klotski.layout.ScreenLayout
import com.dchernykh.klotski.layout.selectionBox
import com.dchernykh.klotski.layout.tileBox

/**
 * The tray, its gate, and the ring around whichever block is selected.
 *
 * One canvas underneath the blocks. The blocks themselves are images and have to
 * be their own composables, so the ring is drawn here rather than with them - it
 * sits outside the block anyway, on the tray.
 */
@Composable
fun TrayCanvas(
    layout: ScreenLayout,
    game: GameState?,
    selected: Int?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val tray = layout.tray
        drawRoundRect(
            color = ColorTray,
            topLeft = Offset(tray.x.toFloat(), tray.y.toFloat()),
            size = Size(tray.w.toFloat(), tray.h.toFloat()),
            cornerRadius = CornerRadius(layout.trayRadius.toFloat()),
        )
        val board = layout.board
        drawRoundRect(
            color = ColorBoard,
            topLeft = Offset(board.x.toFloat(), board.y.toFloat()),
            size = Size(board.w.toFloat(), board.h.toFloat()),
            cornerRadius = CornerRadius(layout.boardRadius.toFloat()),
        )

        // The gate is drawn from the board's bottom edge right through the rim, so
        // it reads as a way out rather than as a stripe on the tray.
        val gate = layout.gate
        drawRect(
            color = ColorExit,
            topLeft = Offset(gate.x.toFloat(), gate.y.toFloat()),
            size = Size(gate.w.toFloat(), gate.h.toFloat()),
        )

        val block = game?.blocks?.getOrNull(selected ?: -1) ?: return@Canvas
        val ring = selectionBox(tileBox(board, block), layout.selection.margin)
        val width = layout.selection.width.toFloat()
        drawRoundRect(
            color = ColorSelection,
            // Inset by half the stroke, which straddles the path it is drawn on:
            // without this the ring would spill half its width over the block.
            topLeft = Offset(ring.x + width / 2f, ring.y + width / 2f),
            size = Size(ring.w - width, ring.h - width),
            cornerRadius = CornerRadius(layout.selection.radius.toFloat()),
            style = Stroke(width = width),
        )
    }
}

/**
 * One block: its portrait, at the cell it currently stands on.
 *
 * The pictures are stored at the size the design cuts them for and drawn scaled to
 * whatever cell this screen ended up with, which is what lets one set of files
 * serve every round watch.
 */
@Composable
fun BlockTile(
    layout: ScreenLayout,
    game: GameState,
    id: Int,
    portrait: Int,
    onClick: () -> Unit,
    label: String,
) {
    val box = tileBox(layout.board, game.blocks[id])
    Box(modifier = Modifier.absoluteBox(box).tileClick(label, onClick)) {
        Image(
            painter = painterResource(portrait),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
}
