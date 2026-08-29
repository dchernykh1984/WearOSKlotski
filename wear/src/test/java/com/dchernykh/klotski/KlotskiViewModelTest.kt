package com.dchernykh.klotski

import com.dchernykh.klotski.game.Direction
import com.dchernykh.klotski.game.LEVELS
import com.dchernykh.klotski.game.Result
import com.dchernykh.klotski.game.UNKNOWN
import com.dchernykh.klotski.game.shortestSolution
import com.dchernykh.klotski.store.RecordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** An in-memory stand-in for the watch's storage. */
private class FakeRecordStore(
    var level: Int = LEVELS.first().id,
    private val bests: MutableMap<Int, Result> = mutableMapOf(),
) : RecordStore {
    var writes = 0
        private set

    override suspend fun readLevel(): Int = level

    override suspend fun writeLevel(levelId: Int) {
        level = levelId
    }

    override suspend fun readBest(levelId: Int): Result = bests[levelId] ?: Result()

    override suspend fun writeBest(
        levelId: Int,
        best: Result,
    ) {
        bests[levelId] = best
        writes++
    }
}

/** A clock the test winds by hand, so a timed game needs no waiting. */
private class FakeClock(
    var now: Long = 1_000L,
) : () -> Long {
    override fun invoke(): Long = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class KlotskiViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(store: RecordStore = FakeRecordStore()) = KlotskiViewModel(store, clock)

    /** Play a board to its solution by replaying the moves that solve it. */
    private fun solve(model: KlotskiViewModel) {
        val game = model.uiState.value.game ?: error("no game to solve")
        for ((id, direction) in shortestSolution(game)!!) {
            model.select(id)
            model.slide(direction)
        }
    }

    @Test
    fun `opens on the start screen, at the board it was left on`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = 3)
            store.writeBest(3, Result(40, 5_000))
            val model = viewModel(store)

            advanceUntilIdle()

            assertEquals(Screen.START, model.uiState.value.screen)
            assertEquals(3, model.uiState.value.levelId)
            assertEquals(Result(40, 5_000), model.uiState.value.best)
        }

    @Test
    fun `walks the ladder of boards and remembers where it stopped`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = 1)
            val model = viewModel(store)
            advanceUntilIdle()

            model.nextBoard()
            advanceUntilIdle()
            assertEquals(2, model.uiState.value.levelId)
            assertEquals(2, store.level)

            model.previousBoard()
            advanceUntilIdle()
            assertEquals(1, model.uiState.value.levelId)
        }

    @Test
    fun `deals the chosen board when a game starts`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()

            model.startGame()

            val state = model.uiState.value
            assertEquals(Screen.PLAYING, state.screen)
            assertEquals(LEVELS.first().blocks, state.game?.blocks)
            assertEquals(0, state.game?.moves)
            assertNull(state.selected)
        }

    @Test
    fun `slides only the block that was picked`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val before = model.uiState.value.game

            // Nothing is picked yet, so a swipe has nothing to move.
            model.slide(Direction.DOWN)
            assertEquals(before, model.uiState.value.game)

            val soldier =
                model.uiState.value.game!!
                    .blocks
                    .indexOfFirst { it.y == 2 }
            model.select(soldier)
            model.slide(Direction.DOWN)

            assertEquals(
                1,
                model.uiState.value.game
                    ?.moves,
            )
        }

    @Test
    fun `keeps the block picked, so a run of swipes pushes the same one`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val soldier =
                model.uiState.value.game!!
                    .blocks
                    .indexOfFirst { it.y == 2 }

            model.select(soldier)
            model.select(soldier)

            assertEquals(soldier, model.uiState.value.selected)
        }

    @Test
    fun `takes a move back, and the counter with it`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val dealt = model.uiState.value.game
            val soldier = dealt!!.blocks.indexOfFirst { it.y == 2 }
            model.select(soldier)
            model.slide(Direction.DOWN)

            model.undo()

            assertEquals(
                dealt.blocks,
                model.uiState.value.game
                    ?.blocks,
            )
            assertEquals(
                0,
                model.uiState.value.game
                    ?.moves,
            )
        }

    @Test
    fun `puts the board back as it started, and the clock with it`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val dealt = model.uiState.value.game
            val soldier = dealt!!.blocks.indexOfFirst { it.y == 2 }
            model.select(soldier)
            model.slide(Direction.DOWN)
            clock.now += 30_000

            model.restart()

            assertEquals(
                dealt.blocks,
                model.uiState.value.game
                    ?.blocks,
            )
            assertEquals(
                0,
                model.uiState.value.game
                    ?.moves,
            )
            assertNull(model.uiState.value.selected)
        }

    @Test
    fun `pauses over the board and comes back to it`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val position = model.uiState.value.game

            model.pauseGame()
            assertEquals(Screen.PAUSED, model.uiState.value.screen)
            assertEquals(position, model.uiState.value.game)

            model.resumeGame()
            assertEquals(Screen.PLAYING, model.uiState.value.screen)
        }

    @Test
    fun `refuses to play out of turn`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()

            model.slide(Direction.DOWN)
            model.undo()
            model.restart()
            model.pauseGame()
            model.resumeGame()

            assertEquals(Screen.START, model.uiState.value.screen)
            assertNull(model.uiState.value.game)
        }

    @Test
    fun `announces a solved board with the moves and the time it took`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()

            clock.now += 12_000
            solve(model)
            advanceUntilIdle()

            val state = model.uiState.value
            assertEquals(Screen.SOLVED, state.screen)
            assertTrue(state.game!!.isSolved)
            assertEquals(LEVELS.first().par, state.game.moves)
            assertEquals(12_000L, state.elapsed)
            assertTrue(state.isRecord)
            assertEquals(1, store.writes)
        }

    @Test
    fun `keeps the record when a later game was longer`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            store.writeBest(LEVELS.first().id, Result(1, 1))
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()
            solve(model)
            advanceUntilIdle()

            assertFalse(model.uiState.value.isRecord)
            assertEquals(Result(1, 1), model.uiState.value.best)
            assertEquals(1, store.writes)
        }

    @Test
    fun `moves on to the next board and deals it`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = 1)
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()
            solve(model)
            advanceUntilIdle()

            model.playNext()
            advanceUntilIdle()

            assertEquals(2, model.uiState.value.levelId)
            assertEquals(2, store.level)
            assertEquals(Screen.PLAYING, model.uiState.value.screen)
            assertEquals(
                LEVELS[1].blocks,
                model.uiState.value.game
                    ?.blocks,
            )
        }

    @Test
    fun `shows a record straight after setting one`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = 1)
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()
            clock.now += 8_000
            solve(model)

            // No idling in between: the read has to queue behind the write, or the
            // records show the value the record just replaced.
            model.showRecords(1)
            advanceUntilIdle()

            assertEquals(LEVELS.first().par, model.uiState.value.recordsBest.moves)
            assertEquals(8_000L, model.uiState.value.recordsBest.time)
        }

    @Test
    fun `pages the records in both directions`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = 1)
            store.writeBest(2, Result(20, 4_000))
            val model = viewModel(store)
            advanceUntilIdle()

            model.showRecords(1)
            advanceUntilIdle()
            assertEquals(Screen.RECORDS, model.uiState.value.screen)
            assertEquals(1, model.uiState.value.recordsId)

            model.recordsNext()
            advanceUntilIdle()
            assertEquals(2, model.uiState.value.recordsId)
            assertEquals(Result(20, 4_000), model.uiState.value.recordsBest)

            model.recordsPrevious()
            advanceUntilIdle()
            assertEquals(1, model.uiState.value.recordsId)
        }

    @Test
    fun `goes back to the start screen and clears the board`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            model.showStart()

            assertEquals(Screen.START, model.uiState.value.screen)
            assertNull(model.uiState.value.game)
            assertNull(model.uiState.value.selected)
            assertFalse(model.uiState.value.isRecord)
        }

    @Test
    fun `leaves a game untimed when the watch's clock moved backwards`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            clock.now -= 5_000
            solve(model)
            advanceUntilIdle()

            assertEquals(Screen.SOLVED, model.uiState.value.screen)
            assertEquals(UNKNOWN, model.uiState.value.elapsed)
            // A game with no clock is still a record on moves alone.
            assertTrue(model.uiState.value.isRecord)
            assertNotNull(model.uiState.value.game)
        }
}
