package com.dchernykh.klotski.layout

import org.junit.Assert.assertTrue
import kotlin.math.hypot

/**
 * The one assertion every layout test wants: that the bezel does not slice a
 * corner off the thing that was just placed.
 *
 * Shared because writing it out means four nested loops - screens, boxes, corners
 * of a box, coordinates of a corner - and four of those in a row is what turns a
 * test file into something nobody reads.
 */
fun assertCornersOnScreen(
    screenSize: Int,
    box: Box,
    what: String,
) {
    val radius = screenSize / 2f
    val corners =
        listOf(
            box.x to box.y,
            box.x + box.w to box.y,
            box.x to box.y + box.h,
            box.x + box.w to box.y + box.h,
        )
    for ((x, y) in corners) {
        assertTrue(
            "corner ($x, $y) of $what escapes a $screenSize screen",
            hypot(x - radius, y - radius) <= radius,
        )
    }
}
