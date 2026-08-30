package com.dchernykh.klotski.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dchernykh.klotski.KlotskiUiState
import com.dchernykh.klotski.KlotskiViewModel
import com.dchernykh.klotski.R
import com.dchernykh.klotski.Screen
import com.dchernykh.klotski.game.Result
import com.dchernykh.klotski.game.formatElapsed
import com.dchernykh.klotski.game.levelById
import com.dchernykh.klotski.game.nextLevel
import com.dchernykh.klotski.game.previousLevel
import com.dchernykh.klotski.layout.ScreenLayout

// The four menus and the records screen. They live one file away from the shell
// that hosts them because they are what changes when the game gains a screen, and
// the shell is what does not.

/** Whichever screen is in front, or none at all while a game is being played. */
@Composable
fun Screens(
    layout: ScreenLayout,
    state: KlotskiUiState,
    viewModel: KlotskiViewModel,
) {
    when (state.screen) {
        Screen.PLAYING -> Unit
        Screen.START -> StartMenu(layout, state, viewModel)
        Screen.PAUSED -> PausedMenu(layout, viewModel)
        Screen.SOLVED -> SolvedMenu(layout, state, viewModel)
        Screen.RECORDS -> RecordsScreen(layout, state, viewModel)
    }
}

@Composable
private fun StartMenu(
    layout: ScreenLayout,
    state: KlotskiUiState,
    viewModel: KlotskiViewModel,
) {
    val text = layout.text
    MenuOverlay(
        layout = layout,
        items =
            listOf(
                MenuItem.Line(text.title, ColorText, stringResource(R.string.app_name)),
                MenuItem.Gap(text.gap),
                MenuItem.Action(text.button, levelName(state.levelId), viewModel::nextBoard),
                MenuItem.Gap(text.gap),
                MenuItem.Action(text.button, stringResource(R.string.play), viewModel::startGame),
                MenuItem.Gap(text.gap),
                MenuItem.Action(text.button, stringResource(R.string.records)) {
                    viewModel.showRecords(state.levelId)
                },
                MenuItem.Line(text.hint, ColorMuted, stringResource(R.string.hint)),
            ),
    )
}

@Composable
private fun PausedMenu(
    layout: ScreenLayout,
    viewModel: KlotskiViewModel,
) {
    val text = layout.text
    MenuOverlay(
        layout = layout,
        items =
            listOf(
                MenuItem.Action(text.button, stringResource(R.string.resume), viewModel::resumeGame),
                MenuItem.Gap(text.gap),
                MenuItem.Action(text.button, stringResource(R.string.levels), viewModel::showStart),
            ),
    )
}

@Composable
private fun SolvedMenu(
    layout: ScreenLayout,
    state: KlotskiUiState,
    viewModel: KlotskiViewModel,
) {
    val text = layout.text
    val moves = state.game?.moves ?: 0
    val record =
        if (state.isRecord) {
            listOf(MenuItem.Line(text.small, ColorAccent, stringResource(R.string.new_best)))
        } else {
            emptyList()
        }
    MenuOverlay(
        layout = layout,
        items =
            listOf(
                MenuItem.Line(text.title, ColorAccent, stringResource(R.string.solved)),
                MenuItem.Gap(text.gap),
                MenuItem.Line(text.row, ColorText, stringResource(R.string.moves_value, moves.toString())),
                MenuItem.Line(text.row, ColorText, stringResource(R.string.time_value, elapsed(state.elapsed))),
            ) + record +
                listOf(
                    MenuItem.Gap(text.gap),
                    MenuItem.Action(text.button, stringResource(R.string.next), viewModel::playNext),
                    MenuItem.Action(text.button, stringResource(R.string.again), viewModel::startGame),
                ),
    )
}

/**
 * One board's record, on the whole round face rather than on a panel.
 *
 * The boards either side are hung above and below it, barely lit, so it is visible
 * that the ladder goes on in both directions - which is also what the swipe does.
 */
@Composable
private fun RecordsScreen(
    layout: ScreenLayout,
    state: KlotskiUiState,
    viewModel: KlotskiViewModel,
) {
    val boxes = layout.records
    val level = levelById(state.recordsId)
    val best = state.recordsBest

    MenuLine(boxes.above, ColorDim, levelName(previousLevel(state.recordsId).id))
    // The board being read is the accent; the two hung either side of it are not.
    MenuLine(boxes.title, ColorAccent, levelName(level.id))
    MenuLine(boxes.rows[0], ColorText, stringResource(R.string.moves_value, movesOrNone(best)))
    MenuLine(boxes.rows[1], ColorText, stringResource(R.string.time_value, elapsed(best.time)))
    MenuLine(boxes.rows[2], ColorMuted, stringResource(R.string.minimum_value, level.par))
    MenuLine(boxes.below, ColorDim, levelName(nextLevel(state.recordsId).id))
    MenuButton(boxes.back, stringResource(R.string.back), viewModel::showStart)
}

@Composable
private fun levelName(levelId: Int) = stringResource(R.string.level_number, levelId)

/** A board with no record yet has no move count to show, only the placeholder. */
@Composable
private fun movesOrNone(best: Result) = if (best.exists) best.moves.toString() else stringResource(R.string.placeholder)

@Composable
private fun elapsed(time: Long) = formatElapsed(time, stringResource(R.string.placeholder))
