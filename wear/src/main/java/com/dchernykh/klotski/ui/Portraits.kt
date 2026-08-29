package com.dchernykh.klotski.ui

import androidx.annotation.DrawableRes
import com.dchernykh.klotski.R
import com.dchernykh.klotski.game.Block
import com.dchernykh.klotski.game.Kind

// Which picture goes on which block.
//
// The faces are details of a Qing dynasty album of Peking opera characters (see
// docs/ASSETS.md, and the files are the originals byte for byte): the hero is the
// commander, the guard is the general lying across the board, and the four
// standing generals and four foot soldiers each get their own portrait so a board
// of ten blocks never looks like ten copies of the same tile.

private val PORTRAITS =
    mapOf(
        Kind.HERO to listOf(R.drawable.hero),
        Kind.GUARD to listOf(R.drawable.guard),
        Kind.GENERAL to
            listOf(R.drawable.general_1, R.drawable.general_2, R.drawable.general_3, R.drawable.general_4),
        Kind.SOLDIER to
            listOf(R.drawable.soldier_1, R.drawable.soldier_2, R.drawable.soldier_3, R.drawable.soldier_4),
    )

/**
 * The portrait for the n-th block of a kind, counting from zero in board order. A
 * board with more blocks of a kind than there are portraits starts over from the
 * first one.
 */
@DrawableRes
fun portraitFor(
    kind: Kind,
    ordinal: Int,
): Int {
    val art = PORTRAITS.getValue(kind)
    return art[(ordinal % art.size + art.size) % art.size]
}

/** Hand every block on a board its portrait, in board order. */
@DrawableRes
fun portraitsFor(blocks: List<Block>): List<Int> {
    val seen = mutableMapOf<Kind, Int>()
    return blocks.map { block ->
        val ordinal = seen.getOrDefault(block.kind, 0)
        seen[block.kind] = ordinal + 1
        portraitFor(block.kind, ordinal)
    }
}
