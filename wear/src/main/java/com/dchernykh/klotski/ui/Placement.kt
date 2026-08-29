package com.dchernykh.klotski.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.dchernykh.klotski.layout.Box as LayoutBox

/**
 * Place a composable at a box worked out in screen pixels.
 *
 * The layout is computed in whole pixels from the screen diameter, exactly as on
 * the watch this was ported from, so it is placed in pixels too: converting each
 * edge to dp and back would round it twice and pull the board off centre.
 */
fun Modifier.absoluteBox(box: LayoutBox): Modifier =
    this
        .offset { IntOffset(box.x, box.y) }
        .layout { measurable, _ ->
            val placeable = measurable.measure(Constraints.fixed(box.w, box.h))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }

/**
 * A block, as something a finger can pick.
 *
 * No ripple: the block is a painted tile and the gold ring around it is what says
 * it is selected, which is also what the Zepp OS original did. The description is
 * all a screen reader has to go on, because the tile itself is a picture.
 */
fun Modifier.tileClick(
    label: String,
    onClick: () -> Unit,
): Modifier =
    this
        .semantics { contentDescription = label }
        .clickable(indication = null, interactionSource = null, role = Role.Button, onClick = onClick)

/** A round control, as something a finger can press, with its own press picture. */
fun Modifier.controlClick(
    interactionSource: MutableInteractionSource,
    label: String,
    onClick: () -> Unit,
): Modifier =
    this
        .semantics { contentDescription = label }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
