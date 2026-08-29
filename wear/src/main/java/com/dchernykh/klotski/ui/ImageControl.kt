package com.dchernykh.klotski.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.dchernykh.klotski.layout.Box as LayoutBox

/**
 * One of the two round controls beside the board: undo and menu.
 *
 * Each is a picture with a second picture for the press, exactly as the Zepp OS
 * original drew them, and the files are the same ones. There is no ripple, because
 * the pressed picture already is the feedback.
 */
@Composable
fun ImageControl(
    box: LayoutBox,
    normal: Int,
    pressed: Int,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(modifier = Modifier.absoluteBox(box).controlClick(interactionSource, label, onClick)) {
        Image(
            painter = painterResource(if (isPressed) pressed else normal),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
