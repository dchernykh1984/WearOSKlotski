package com.dchernykh.klotski

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dchernykh.klotski.game.Direction
import com.dchernykh.klotski.game.FIRST_LEVEL
import com.dchernykh.klotski.game.GameState
import com.dchernykh.klotski.game.Result
import com.dchernykh.klotski.game.UNKNOWN
import com.dchernykh.klotski.game.elapsedBetween
import com.dchernykh.klotski.game.levelById
import com.dchernykh.klotski.game.moved
import com.dchernykh.klotski.game.newGame
import com.dchernykh.klotski.game.nextLevel
import com.dchernykh.klotski.game.previousLevel
import com.dchernykh.klotski.game.restarted
import com.dchernykh.klotski.game.undone
import com.dchernykh.klotski.game.updateBest
import com.dchernykh.klotski.store.RecordStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which of the five screens is in front. */
enum class Screen { START, PLAYING, PAUSED, SOLVED, RECORDS }

/**
 * Everything the screen draws.
 *
 * [game] is the whole position, which is an immutable value, so Compose sees a new
 * one after every move and nothing can mutate the board being painted.
 */
data class KlotskiUiState(
    val screen: Screen = Screen.START,
    val levelId: Int = FIRST_LEVEL,
    val best: Result = Result(),
    val game: GameState? = null,
    val selected: Int? = null,
    val elapsed: Long = UNKNOWN,
    val isRecord: Boolean = false,
    val recordsId: Int = FIRST_LEVEL,
    val recordsBest: Result = Result(),
)

/**
 * The game as the screen sees it.
 *
 * [now] is injected rather than read from the system, which is the whole of what
 * makes a timed game testable: a test hands it a clock it controls instead of
 * waiting out a real one.
 */
class KlotskiViewModel(
    private val store: RecordStore,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KlotskiUiState())
    val uiState: StateFlow<KlotskiUiState> = _uiState.asStateFlow()

    /** When the board in front was opened. A game is timed end to end; see Clock. */
    private var startedAt: Long = 0

    // Every touch of the settings goes through this, each waiting on the one
    // before. Without it the first read - which starts as soon as the view model
    // does - can finish after a tap that chose a board, and quietly put the stored
    // one back over the one the player just picked.
    private var settings: Job = Job().apply { complete() }

    init {
        settings =
            viewModelScope.launch {
                val levelId = store.readLevel()
                _uiState.update {
                    it.copy(levelId = levelId, best = store.readBest(levelId), recordsId = levelId)
                }
            }
    }

    /** Walk to another board and remember it, so the game reopens where it was left. */
    fun chooseLevel(levelId: Int) {
        val previous = settings
        settings =
            viewModelScope.launch {
                previous.join()
                store.writeLevel(levelId)
                _uiState.update { it.copy(levelId = levelId, best = store.readBest(levelId)) }
            }
    }

    fun nextBoard() = chooseLevel(nextLevel(_uiState.value.levelId).id)

    fun previousBoard() = chooseLevel(previousLevel(_uiState.value.levelId).id)

    fun startGame() {
        startedAt = now()
        _uiState.update {
            it.copy(
                screen = Screen.PLAYING,
                game = newGame(levelById(it.levelId)),
                selected = null,
                elapsed = UNKNOWN,
                isRecord = false,
            )
        }
    }

    /**
     * Pick the block a swipe will move. Tapping the same block again is a natural
     * thing to do halfway through sliding it along, so it keeps the block rather
     * than toggling the selection off and swallowing the next swipe.
     */
    fun select(id: Int) {
        if (_uiState.value.screen != Screen.PLAYING) return
        _uiState.update { it.copy(selected = id) }
    }

    /** Slide the selected block one cell. A move the rules refuse does nothing. */
    fun slide(direction: Direction) {
        val state = _uiState.value
        if (state.screen != Screen.PLAYING) return
        val game = state.game ?: return
        val id = state.selected ?: return
        val next = game.moved(id, direction)
        if (next === game) return
        _uiState.update { it.copy(game = next) }
        if (next.isSolved) finish(next)
    }

    fun undo() {
        if (_uiState.value.screen != Screen.PLAYING) return
        _uiState.update { it.copy(game = it.game?.undone()) }
    }

    /** Put the board back as it started, and the clock with it. */
    fun restart() {
        if (_uiState.value.screen != Screen.PLAYING) return
        startedAt = now()
        _uiState.update { it.copy(game = it.game?.restarted(), selected = null) }
    }

    /** The menu pauses over the board, so the position is still there to come back to. */
    fun pauseGame() {
        if (_uiState.value.screen != Screen.PLAYING) return
        _uiState.update { it.copy(screen = Screen.PAUSED) }
    }

    fun resumeGame() {
        if (_uiState.value.screen != Screen.PAUSED) return
        _uiState.update { it.copy(screen = Screen.PLAYING) }
    }

    fun showStart() {
        _uiState.update {
            it.copy(screen = Screen.START, game = null, selected = null, isRecord = false)
        }
    }

    fun showRecords(levelId: Int) {
        viewModelScope.launch {
            val best = store.readBest(levelId)
            _uiState.update { it.copy(screen = Screen.RECORDS, recordsId = levelId, recordsBest = best) }
        }
    }

    fun recordsNext() = showRecords(nextLevel(_uiState.value.recordsId).id)

    fun recordsPrevious() = showRecords(previousLevel(_uiState.value.recordsId).id)

    /** The board after this one, opened and dealt: what "Next" does on the solved screen. */
    fun playNext() {
        val next = nextLevel(_uiState.value.levelId).id
        val previous = settings
        settings =
            viewModelScope.launch {
                previous.join()
                store.writeLevel(next)
                _uiState.update { it.copy(levelId = next, best = store.readBest(next)) }
                startGame()
            }
    }

    /**
     * The hero is out. The board is left on screen under the panel, and a record is
     * written only when there is one, so an ordinary game never touches storage.
     */
    private fun finish(solved: GameState) {
        val elapsed = elapsedBetween(startedAt, now())
        val result = Result(moves = solved.moves, time = elapsed)
        val state = _uiState.value
        val outcome = updateBest(state.best, result)
        _uiState.update {
            it.copy(
                screen = Screen.SOLVED,
                // The board stays on screen under the panel, but nothing is
                // picked any more: a gold ring around a block on a finished
                // puzzle reads as a move still waiting to be made.
                selected = null,
                elapsed = elapsed,
                best = outcome.best,
                isRecord = outcome.isRecord,
            )
        }
        if (!outcome.isRecord) return
        viewModelScope.launch {
            // Not cancellable. The app being closed the instant a puzzle falls is
            // exactly when a record is worth keeping, and a write abandoned half
            // way through loses it for good.
            withContext(NonCancellable) { store.writeBest(state.levelId, outcome.best) }
        }
    }

    companion object {
        fun factory(store: RecordStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return KlotskiViewModel(store) as T
                }
            }
    }
}
