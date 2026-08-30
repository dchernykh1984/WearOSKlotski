package com.dchernykh.klotski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.dchernykh.klotski.layout.ScreenLayout
import com.dchernykh.klotski.layout.centeredBox
import kotlin.math.roundToInt
import com.dchernykh.klotski.layout.Box as LayoutBox

// The menus: a stack of lines and buttons centred on the board, under a panel dark
// enough to read over the tray without hiding it. Every size comes from the
// layout, which is scaled from the screen, so the same stack fills the same
// proportion of a 384px watch and a 466px one.

/** Below this a label is unreadable on a watch, so a tight box clips rather than shrink further. */
const val MIN_TEXT_PX = 12f

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
        FittedText(text = text, color = color, boxHeight = box.h, boxWidth = box.w, fraction = 0.76f)
    }
}

/**
 * Text sized to fit the box it is in.
 *
 * A font size is not a line height: ascenders, descenders and the leading a font
 * asks for on top of them can want half as much room again as the size does, and
 * how much more depends on the font and on the language. Asking for a size that is
 * a fixed share of the box therefore does not fit in the box - it fits sometimes,
 * and the rest of the time the glyphs are cut off at the bottom, which is what the
 * hint under the start menu was doing in every language.
 *
 * So the share is a ceiling rather than a size: the text is measured, and stepped
 * down only as far as the real glyphs require.
 */
@Composable
fun FittedText(
    text: String,
    color: Color,
    boxHeight: Int,
    boxWidth: Int,
    fraction: Float,
) {
    val density = LocalDensity.current
    val ceilingPx = maxOf(boxHeight * fraction, MIN_TEXT_PX)
    BasicText(
        text = text,
        modifier = Modifier.absoluteWidth(boxWidth),
        style = TextStyle(color = color, textAlign = TextAlign.Center),
        maxLines = 1,
        autoSize =
            TextAutoSize.StepBased(
                minFontSize = with(density) { MIN_TEXT_PX.toSp() },
                maxFontSize = with(density) { ceilingPx.toSp() },
            ),
    )
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
        FittedText(text = text, color = ColorText, boxHeight = box.h, boxWidth = box.w, fraction = 0.46f)
    }
}
