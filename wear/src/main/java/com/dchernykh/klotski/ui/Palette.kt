package com.dchernykh.klotski.ui

import androidx.compose.ui.graphics.Color

// The colours, carried over unchanged from the Zepp OS original so the two
// versions of the game look like the same game.
//
// The board is meant to read as a lacquered wooden tray with painted tiles on it,
// so everything that is not a portrait is a warm dark tone and the only bright
// accents are the gold of the exit and of the selected block. On the OLED a watch
// uses, black is not a colour but pixels that are switched off, which is why the
// ground it all sits on is black and not merely fashionably dark.

val ColorBackground = Color(0xFF000000)
val ColorTray = Color(0xFF2A2019)
val ColorBoard = Color(0xFF120D09)
val ColorExit = Color(0xFFC8A24A)
val ColorSelection = Color(0xFFFFD76A)
val ColorText = Color(0xFFF2ECE0)
val ColorMuted = Color(0xFF9A8F7F)

/**
 * Barely lit: the boards standing above and below the one being read on the
 * records screen, there to show the ladder goes on rather than to be read.
 */
val ColorDim = Color(0xFF5A5148)

val ColorAccent = Color(0xFFE8CF9A)
val ColorButton = Color(0xFF241C16)
val ColorButtonPressed = Color(0xFF3D3025)

/** The panel a menu is drawn on, over a board that stays visible behind it. */
const val PANEL_ALPHA = 225f / 255f
