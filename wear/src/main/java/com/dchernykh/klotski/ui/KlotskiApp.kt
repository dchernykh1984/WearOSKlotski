package com.dchernykh.klotski.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.dchernykh.klotski.KlotskiUiState
import com.dchernykh.klotski.KlotskiViewModel
import com.dchernykh.klotski.R
import com.dchernykh.klotski.Screen
import com.dchernykh.klotski.game.Direction
import com.dchernykh.klotski.layout.ScreenLayout
import com.dchernykh.klotski.layout.cellAt
import com.dchernykh.klotski.layout.screenLayout
import kotlin.math.abs

/**
 * The whole screen: the tray, the blocks on it, the controls around it, and
 * whichever menu is in front.
 *
 * The layout is worked out once from the screen diameter and then everything is
 * placed at absolute pixels, which is what keeps the port looking like the game it
 * was ported from on any round watch.
 */
@Composable
fun KlotskiApp(viewModel: KlotskiViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The screen is a circle, so one diameter drives every measurement. Taking the
    // smaller side keeps that true even if a watch reports a pixel of slop between
    // width and height.
    val container = LocalWindowInfo.current.containerSize
    val screenSize = minOf(container.width, container.height)
    // Every measurement below divides the screen up, and a screen of no size
    // divides into negative boxes rather than into nothing.
    if (screenSize <= 0) return

    val layout = remember(screenSize) { screenLayout(screenSize) }

    KeepScreenOnWhile(state.screen == Screen.PLAYING || state.screen == Screen.PAUSED)

    // Wear OS reads a swipe from the left edge as Back. During a game that must not
    // leave the app - sliding a block right would end the session - so it opens the
    // menu instead, which is the one thing a player pressing Back mid-game could
    // want. From a menu it steps back towards the start screen, and from the start
    // screen it is left alone so the watch closes the app as it does any other.
    BackHandler(enabled = state.screen != Screen.START) { goBack(state.screen, viewModel) }

    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ColorBackground)
                    .swipes(state.screen) { onSwipe(state.screen, it, viewModel) }
                    .taps(layout, state.screen, viewModel),
        ) {
            TrayCanvas(layout, state.game, state.selected, Modifier.fillMaxSize())
            Blocks(layout, state, viewModel)

            if (state.screen == Screen.PLAYING) {
                Counter(layout, state)
                PlayControls(layout, viewModel)
            }

            Screens(layout, state, viewModel)
        }
    }
}

/** What Back does, which depends on where it was pressed. */
private fun goBack(
    screen: Screen,
    viewModel: KlotskiViewModel,
) {
    when (screen) {
        Screen.PLAYING -> viewModel.pauseGame()
        Screen.PAUSED, Screen.SOLVED, Screen.RECORDS -> viewModel.showStart()
        Screen.START -> Unit
    }
}

/**
 * What a swipe does, which also depends on where it was made.
 *
 * During a game it slides the block that was tapped. On the start screen and on
 * the records it walks the ladder of boards, and it drags them past the window
 * rather than moving a cursor over them - pulling down brings the earlier board
 * into view, pushing up brings the later one - which is what a finger expects of
 * any list.
 */
private fun onSwipe(
    screen: Screen,
    direction: Direction,
    viewModel: KlotskiViewModel,
) {
    when (screen) {
        Screen.PLAYING -> viewModel.slide(direction)
        Screen.START ->
            when (direction) {
                Direction.DOWN -> viewModel.previousBoard()
                Direction.UP -> viewModel.nextBoard()
                else -> Unit
            }
        Screen.RECORDS ->
            when (direction) {
                Direction.DOWN -> viewModel.recordsPrevious()
                Direction.UP -> viewModel.recordsNext()
                else -> Unit
            }
        Screen.PAUSED, Screen.SOLVED -> Unit
    }
}

/**
 * A game outlasts the watch's display timeout by a wide margin, and a puzzle is
 * solved in long silences with nothing touching the screen. The menu counts too:
 * it is paused over a board somebody is still looking at.
 */
@Composable
private fun KeepScreenOnWhile(playing: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Swipes, over the whole screen.
 *
 * A drag is read once, on the first movement that clears the touch slop, and the
 * longer axis decides: a swipe is never exactly straight, and waiting for the
 * finger to lift would slide the block a moment too late to feel connected to it.
 *
 * Keyed on [screen] so that [onSwipe] is read afresh whenever the screen changes.
 * A pointerInput block keyed on Unit would hold on to the very first one for the
 * life of the app.
 */
private fun Modifier.swipes(
    screen: Screen,
    onSwipe: (Direction) -> Unit,
): Modifier =
    pointerInput(screen) {
        var handled = false
        detectDragGestures(
            onDragStart = { handled = false },
            onDragEnd = { handled = false },
            onDragCancel = { handled = false },
        ) { change, drag ->
            change.consume()
            if (!handled) {
                handled = true
                onSwipe(
                    if (abs(drag.x) > abs(drag.y)) {
                        if (drag.x > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (drag.y > 0) Direction.DOWN else Direction.UP
                    },
                )
            }
        }
    }

/**
 * A tap on the tray picks the block under the finger.
 *
 * The blocks carry their own click for a screen reader to find, but a tap has to
 * work on the gap between two blocks as well - the inset that makes them read as
 * separate pieces is a real few pixels, and a tap landing in it should still pick
 * the block whose cell it is.
 */
private fun Modifier.taps(
    layout: ScreenLayout,
    screen: Screen,
    viewModel: KlotskiViewModel,
): Modifier =
    pointerInput(layout, screen) {
        if (screen != Screen.PLAYING) return@pointerInput
        detectTapGestures { offset ->
            val cell = cellAt(layout.board, offset.x.toInt(), offset.y.toInt()) ?: return@detectTapGestures
            viewModel.selectAt(cell.first, cell.second)
        }
    }

@Composable
private fun Counter(
    layout: ScreenLayout,
    state: KlotskiUiState,
) {
    val box = layout.counter
    Box(modifier = Modifier.absoluteBox(box), contentAlignment = Alignment.Center) {
        Text(
            text = (state.game?.moves ?: 0).toString(),
            color = ColorText,
            fontSize = with(LocalDensity.current) { (box.h * 0.8f).toSp() },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Blocks(
    layout: ScreenLayout,
    state: KlotskiUiState,
    viewModel: KlotskiViewModel,
) {
    val game = state.game ?: return
    val portraits = remember(game.level.id) { portraitsFor(game.blocks) }
    game.blocks.forEachIndexed { id, _ ->
        BlockTile(
            layout = layout,
            game = game,
            id = id,
            portrait = portraits[id],
            onClick = { viewModel.select(id) },
            label = stringResource(R.string.level_number, game.level.id),
        )
    }
}

@Composable
private fun PlayControls(
    layout: ScreenLayout,
    viewModel: KlotskiViewModel,
) {
    ImageControl(
        box = layout.undo,
        normal = R.drawable.undo,
        pressed = R.drawable.undo_press,
        label = stringResource(R.string.undo),
        onClick = viewModel::undo,
    )
    ImageControl(
        box = layout.menu,
        normal = R.drawable.menu,
        pressed = R.drawable.menu_press,
        label = stringResource(R.string.menu),
        onClick = viewModel::pauseGame,
    )
    MenuButton(layout.restart, stringResource(R.string.restart), viewModel::restart)
}
