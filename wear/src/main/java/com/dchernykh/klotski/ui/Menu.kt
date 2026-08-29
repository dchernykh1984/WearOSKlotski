package com.dchernykh.klotski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.dchernykh.klotski.layout.ScreenLayout
import com.dchernykh.klotski.layout.centeredBox
import kotlin.math.roundToInt
import com.dchernykh.klotski.layout.Box as LayoutBox

// The menus: a stack of lines and buttons centred on the board, under a panel dark
// enough to read over the tray without hiding it. Every size comes from the
// layout, which is scaled from the screen, so the same stack fills the same
// proportion of a 384px watch and a 466px one.

sealed interface MenuItem {
    val height: Int

    data class Line(
        override val height: Int,
        val color: Color,
        val text: String,
    ) : MenuItem

    data class Action(
        override val height: Int,
        val text: String,
        val onClick: () -> Unit,
    ) : MenuItem

    data class Gap(
        override val height: Int,
    ) : MenuItem
}

@Composable
fun MenuOverlay(
    layout: ScreenLayout,
    items: List<MenuItem>,
) {
    val board = layout.board
    val gap = layout.text.gap
    val stackHeight = items.sumOf { it.height }
    val top = board.y + ((board.h - stackHeight) / 2f).roundToInt()
    val panelTop = maxOf(board.y, top - gap)
    val panelHeight = minOf(board.h, stackHeight + 2 * gap)

    Box(
        modifier =
            Modifier
                .absoluteBox(LayoutBox(board.x, panelTop, board.w, panelHeight))
                // In pixels, like every other measurement here. As dp it would be
                // scaled by the watch's density and come out far rounder than the
                // tray it sits inside.
                .clip(RoundedCornerShape(with(LocalDensity.current) { layout.boardRadius.toDp() }))
                // Not opaque: the board stays visible behind the menu, which is
                // what makes pausing feel like putting a puzzle down rather than
                // leaving it.
                .background(ColorBackground.copy(alpha = PANEL_ALPHA)),
    )

    var y = top
    for (item in items) {
        val box = centeredBox(layout.screenSize, y, item.height, layout.menuWidth.toFloat(), layout.padding)
        when (item) {
            is MenuItem.Gap -> Unit
            is MenuItem.Line -> MenuLine(box, item.color, item.text)
            is MenuItem.Action -> MenuButton(box, item.text, item.onClick)
        }
        y += item.height
    }
}

@Composable
fun MenuLine(
    box: LayoutBox,
    color: Color,
    text: String,
) {
    Box(modifier = Modifier.absoluteBox(box), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = color,
            fontSize = with(LocalDensity.current) { (box.h * 0.76f).toSp() },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MenuButton(
    box: LayoutBox,
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier =
            Modifier
                .absoluteBox(box)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (pressed) ColorButtonPressed else ColorButton)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    // Without the role a screen reader reads the label as plain
                    // text, and nothing tells the listener it can be pressed.
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = ColorText,
            fontSize = with(LocalDensity.current) { (box.h * 0.46f).toSp() },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
