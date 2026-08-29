package com.dchernykh.klotski.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dchernykh.klotski.game.FIRST_LEVEL
import com.dchernykh.klotski.game.Result
import com.dchernykh.klotski.game.levelById
import com.dchernykh.klotski.game.normalizeResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * What survives closing the app: the board last played, and one record per board.
 *
 * A record is stored as two values rather than one packed one - the moves and the
 * clock - because two keys need no parsing and cannot half-decode.
 *
 * An interface, because everything interesting happens above it: the view model is
 * driven through this, so a JVM test can play whole games against an in-memory
 * implementation instead of an emulator.
 */
interface RecordStore {
    suspend fun readLevel(): Int

    suspend fun writeLevel(levelId: Int)

    suspend fun readBest(levelId: Int): Result

    suspend fun writeBest(
        levelId: Int,
        best: Result,
    )
}

private val Context.recordDataStore: DataStore<Preferences> by preferencesDataStore(name = "records")

private val LEVEL_KEY = intPreferencesKey("level")

private fun movesKey(levelId: Int) = intPreferencesKey("best.$levelId")

private fun timeKey(levelId: Int) = longPreferencesKey("time.$levelId")

/**
 * The real store, on top of Preferences DataStore.
 *
 * Storage that has gone wrong must not stop anyone playing: a failed read reads as
 * nothing stored and a failed write is dropped, so a corrupt preferences file
 * costs a record rather than the app.
 */
class DataStoreRecordStore(
    context: Context,
) : RecordStore {
    // The application context, not the activity's: a DataStore outlives any one
    // screen, and holding the activity here would leak it for the life of the app.
    private val dataStore = context.applicationContext.recordDataStore

    private suspend fun read(): Preferences =
        dataStore.data
            .catch { cause ->
                // Only I/O. Anything else is a bug in this file rather than a
                // broken disk, and swallowing it would hide it.
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }.first()

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit(change)
        } catch (_: IOException) {
            // Nothing to do and nothing worth saying: the game carries on.
        }
    }

    // levelById turns a board number from an older version into one that exists,
    // so a stored choice can never leave the game with nothing to play.
    override suspend fun readLevel(): Int = levelById(read()[LEVEL_KEY] ?: FIRST_LEVEL).id

    override suspend fun writeLevel(levelId: Int) = write { it[LEVEL_KEY] = levelId }

    override suspend fun readBest(levelId: Int): Result {
        val stored = read()
        return normalizeResult(
            Result(stored[movesKey(levelId)] ?: 0, stored[timeKey(levelId)] ?: 0L),
        )
    }

    override suspend fun writeBest(
        levelId: Int,
        best: Result,
    ) = write {
        val clean = normalizeResult(best)
        it[movesKey(levelId)] = clean.moves
        it[timeKey(levelId)] = clean.time
    }
}
