package com.dchernykh.klotski.layout

import com.dchernykh.klotski.game.BOARD_COLS
import com.dchernykh.klotski.game.BOARD_ROWS
import com.dchernykh.klotski.game.GOAL
import com.dchernykh.klotski.game.Kind
import kotlin.math.roundToInt

// Where everything sits on the watch face. Pure arithmetic, so the whole screen
// can be checked in unit tests.
//
// Every measurement is drawn for one design and then scaled to the diameter of
// the screen it lands on. That matters more than it sounds: round Wear OS watches
// run from about 384px to 480px, and a board pinned to one cell size would hang
// off the bezel on the smallest of them and leave the counter and the buttons
// with nowhere to go.

/** The screen every measurement below is drawn for; any other size is this one scaled. */
const val DESIGN_SIZE = 466

/** The cell of the design, and the smallest cell worth drawing a portrait in. */
const val DESIGN_CELL = 60
const val MIN_CELL = 24

/** Pixels trimmed off each side of a cell, so blocks read as pieces and not a wall. */
const val DESIGN_TILE_GAP = 2

/** The tray the blocks slide in: how far its rim stands out, and how round it is. */
const val DESIGN_TRAY_MARGIN = 6
const val DESIGN_TRAY_RADIUS = 14

/** The ring drawn around the selected block, just outside it. */
const val DESIGN_SELECTION_MARGIN = 1
const val DESIGN_SELECTION_RADIUS = 8
const val DESIGN_SELECTION_WIDTH = 3

const val DESIGN_SCREEN_PADDING = 8

/** Round controls sit in the margins beside the board, where nothing covers them. */
const val DESIGN_BUTTON_SIZE = 56
const val DESIGN_BUTTON_GAP = 12
const val DESIGN_WIDE_BUTTON_W = 140
const val DESIGN_WIDE_BUTTON_H = 44

/**
 * The move counter sits high in the round cap, where the screen has already
 * narrowed. It only ever holds a couple of numbers, so it is kept well inside the
 * chord rather than stretched to the width of the board.
 */
const val DESIGN_COUNTER_W = 200
const val DESIGN_COUNTER_H = 36

/** A menu is drawn on a panel over the board; its rows stop short of its corners. */
const val DESIGN_MENU_INSET = 8

/** Moves, time and minimum: the three lines that make up one board's record. */
const val RECORD_LINES = 3

/** The type scale of the menus, and the smallest size still worth reading. */
const val DESIGN_TEXT_TITLE = 40
const val DESIGN_TEXT_ROW = 30
const val DESIGN_TEXT_SMALL = 24
const val DESIGN_TEXT_HINT = 22
const val DESIGN_TEXT_BUTTON = 46
const val DESIGN_TEXT_GAP = 10
const val MIN_TEXT = 12

/**
 * The records screen is the one screen with nothing behind it worth showing: it is
 * paged, not played, so it drops the panel and uses the whole round face. That
 * buys type half again as large as a menu row, and room to hang the boards either
 * side of this one, dimmed, so it is visible that the ladder goes on above and
 * below. The vertical positions are fractions of the diameter rather than pixels
 * of the design, because what has to hold on every screen is where these lines sit
 * in the circle, not how far apart they are.
 */
private const val RECORDS_ROW = 34
private const val RECORDS_NEIGHBOUR = 26
private const val RECORDS_GAP = 16
private const val RECORDS_BACK_WIDTH = 196
private const val RECORDS_ABOVE_CENTER = 0.13f
private const val RECORDS_BLOCK_CENTER = 0.44f
private const val RECORDS_BELOW_CENTER = 0.75f
private const val RECORDS_BACK_TOP = 0.82f

/** The board: where it sits, and the cell and gap every tile box is worked out from. */
data class BoardBox(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val cell: Int,
    val gap: Int,
)

/** How thick the selection ring is and how far outside the block it is drawn. */
data class Selection(
    val margin: Int,
    val radius: Int,
    val width: Int,
)

/** The type scale, in pixels of this screen. */
data class TextScale(
    val title: Int,
    val row: Int,
    val small: Int,
    val hint: Int,
    val button: Int,
    val gap: Int,
)

/** Every line of the records screen, top to bottom. */
data class RecordsScreen(
    val above: Box,
    val title: Box,
    val rows: List<Box>,
    val below: Box,
    val back: Box,
)

/** Every box the screen draws into, for a watch of this size. */
data class ScreenLayout(
    val screenSize: Int,
    val board: BoardBox,
    val padding: Int,
    val tray: Box,
    val trayRadius: Int,
    val boardRadius: Int,
    val gate: Box,
    val selection: Selection,
    val menuWidth: Int,
    val counter: Box,
    val undo: Box,
    val menu: Box,
    val restart: Box,
    val records: RecordsScreen,
    val text: TextScale,
)

/** How much of the design fits on this screen. */
fun scaleFor(screenSize: Int): Float = screenSize / DESIGN_SIZE.toFloat()

private fun at(
    scale: Float,
    value: Int,
    minimum: Int = 1,
): Int = maxOf(minimum, (value * scale).roundToInt())

private fun recordsScreen(
    screenSize: Int,
    scale: Float,
    padding: Int,
): RecordsScreen {
    val title = at(scale, DESIGN_TEXT_TITLE, MIN_TEXT)
    val row = at(scale, RECORDS_ROW, MIN_TEXT)
    val neighbour = at(scale, RECORDS_NEIGHBOUR, MIN_TEXT)
    val gap = at(scale, RECORDS_GAP)

    // Full width, so only the circle decides how wide a line may be.
    fun line(
        top: Int,
        height: Int,
        width: Int = screenSize,
    ) = centeredBox(screenSize, top, height, width.toFloat(), padding)

    val blockHeight = title + gap + RECORD_LINES * row
    val blockTop = (screenSize * RECORDS_BLOCK_CENTER - blockHeight / 2f).roundToInt()

    return RecordsScreen(
        above = line((screenSize * RECORDS_ABOVE_CENTER - neighbour / 2f).roundToInt(), neighbour),
        title = line(blockTop, title),
        rows = List(RECORD_LINES) { i -> line(blockTop + title + gap + i * row, row) },
        below = line((screenSize * RECORDS_BELOW_CENTER - neighbour / 2f).roundToInt(), neighbour),
        back =
            line(
                (screenSize * RECORDS_BACK_TOP).roundToInt(),
                at(scale, DESIGN_WIDE_BUTTON_H),
                at(scale, RECORDS_BACK_WIDTH),
            ),
    )
}

/** The tray of cells, centred on the screen. */
private fun boardFor(
    screenSize: Int,
    scale: Float,
): BoardBox {
    val center = screenSize / 2f
    val cell = maxOf(MIN_CELL, (DESIGN_CELL * scale).roundToInt())
    val width = BOARD_COLS * cell
    val height = BOARD_ROWS * cell
    return BoardBox(
        x = (center - width / 2f).roundToInt(),
        y = (center - height / 2f).roundToInt(),
        w = width,
        h = height,
        cell = cell,
        gap = at(scale, DESIGN_TILE_GAP),
    )
}

private fun selectionFor(scale: Float) =
    Selection(
        margin = at(scale, DESIGN_SELECTION_MARGIN),
        radius = at(scale, DESIGN_SELECTION_RADIUS),
        width = at(scale, DESIGN_SELECTION_WIDTH),
    )

private fun textFor(scale: Float) =
    TextScale(
        title = at(scale, DESIGN_TEXT_TITLE, MIN_TEXT),
        row = at(scale, DESIGN_TEXT_ROW, MIN_TEXT),
        small = at(scale, DESIGN_TEXT_SMALL, MIN_TEXT),
        hint = at(scale, DESIGN_TEXT_HINT, MIN_TEXT),
        button = at(scale, DESIGN_TEXT_BUTTON, MIN_TEXT),
        gap = at(scale, DESIGN_TEXT_GAP),
    )

/** Every box the screen draws, for a watch of the given size. */
fun screenLayout(screenSize: Int): ScreenLayout {
    val scale = scaleFor(screenSize)
    val board = boardFor(screenSize, scale)
    val trayMargin = at(scale, DESIGN_TRAY_MARGIN)
    val trayRadius = at(scale, DESIGN_TRAY_RADIUS)
    val buttonSize = at(scale, DESIGN_BUTTON_SIZE)
    val buttonGap = at(scale, DESIGN_BUTTON_GAP)
    val padding = at(scale, DESIGN_SCREEN_PADDING)
    val counterHeight = at(scale, DESIGN_COUNTER_H)
    val buttonTop = (screenSize / 2f - buttonSize / 2f).roundToInt()

    return ScreenLayout(
        screenSize = screenSize,
        board = board,
        padding = padding,
        tray =
            Box(
                x = board.x - trayMargin,
                y = board.y - trayMargin,
                w = board.w + 2 * trayMargin,
                h = board.h + 2 * trayMargin,
            ),
        trayRadius = trayRadius,
        boardRadius = maxOf(1, trayRadius - trayMargin),
        // The gate is drawn from the board's bottom edge right through the rim, so
        // it reads as a way out rather than as a stripe.
        gate =
            Box(
                x = board.x + GOAL.x * board.cell,
                y = board.y + board.h,
                w = Kind.HERO.w * board.cell,
                h = trayMargin,
            ),
        selection = selectionFor(scale),
        menuWidth = board.w - 2 * at(scale, DESIGN_MENU_INSET),
        counter =
            centeredBox(
                screenSize,
                board.y - buttonGap - counterHeight,
                counterHeight,
                at(scale, DESIGN_COUNTER_W).toFloat(),
                padding,
            ),
        undo = Box(board.x - buttonGap - buttonSize, buttonTop, buttonSize, buttonSize),
        menu = Box(board.x + board.w + buttonGap, buttonTop, buttonSize, buttonSize),
        restart =
            centeredBox(
                screenSize,
                board.y + board.h + buttonGap,
                at(scale, DESIGN_WIDE_BUTTON_H),
                at(scale, DESIGN_WIDE_BUTTON_W).toFloat(),
                padding,
            ),
        records = recordsScreen(screenSize, scale, padding),
        text = textFor(scale),
    )
}
