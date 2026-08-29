package com.dchernykh.klotski

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.dchernykh.klotski.game.LEVELS
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/**
 * What no JVM test can check: that the game actually runs on a watch.
 *
 * Launching the activity exercises the manifest, the theme, the launcher icon, the
 * ten block portraits, the whole Compose tree and the DataStore-backed record
 * store in one go - the parts excused from the coverage floor precisely because
 * they need a device. The rules and the boards are covered far more cheaply by the
 * unit tests, so this walks the screens rather than trying to solve anything.
 *
 * Every label is read from the resources rather than written out, so the test says
 * the same thing on a watch set to any of the eleven languages.
 */
class GameScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = rule.activity.getString(id)

    private fun boardName(levelId: Int) = rule.activity.getString(R.string.level_number, levelId)

    private fun onScreen(label: String) = rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun opensOnTheStartScreen() {
        rule.onNodeWithText(text(R.string.app_name)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.play)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.records)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.hint)).assertIsDisplayed()
    }

    @Test
    fun walksTheLadderOfBoards() {
        rule.waitUntil { LEVELS.any { onScreen(boardName(it.id)) } }
        val before = LEVELS.first { onScreen(boardName(it.id)) }.id

        rule.onNodeWithText(boardName(before)).performClick()
        rule.waitUntil { !onScreen(boardName(before)) }

        assertNotEquals(before, LEVELS.first { onScreen(boardName(it.id)) }.id)
    }

    @Test
    fun walksTheLadderOnAVerticalSwipeToo() {
        rule.waitUntil { LEVELS.any { onScreen(boardName(it.id)) } }
        val before = LEVELS.first { onScreen(boardName(it.id)) }.id

        rule.onNodeWithText(text(R.string.app_name)).performTouchInput { swipeUp() }
        rule.waitUntil { !onScreen(boardName(before)) }

        assertNotEquals(before, LEVELS.first { onScreen(boardName(it.id)) }.id)
    }

    @Test
    fun startsAGameAndShowsItsControls() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription(text(R.string.undo)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.menu)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.restart)).assertIsDisplayed()
    }

    @Test
    fun pausesOverTheBoardAndComesBack() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription(text(R.string.menu)).performClick()
        rule.onNodeWithText(text(R.string.resume)).assertIsDisplayed()

        rule.onNodeWithText(text(R.string.resume)).performClick()
        rule.waitUntil { !onScreen(text(R.string.resume)) }
        rule.onNodeWithContentDescription(text(R.string.menu)).assertIsDisplayed()
    }

    @Test
    fun readsTheRecordsAndComesBack() {
        rule.onNodeWithText(text(R.string.records)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(text(R.string.back)).assertIsDisplayed()
        // Minimum is the one figure a board always has, record or no record.
        rule
            .onNodeWithText(rule.activity.getString(R.string.minimum_value, LEVELS.first().par))
            .assertIsDisplayed()

        rule.onNodeWithText(text(R.string.back)).performClick()
        rule.waitUntil { onScreen(text(R.string.play)) }
    }
}
