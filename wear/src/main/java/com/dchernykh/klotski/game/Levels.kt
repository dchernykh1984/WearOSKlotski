package com.dchernykh.klotski.game

// The bundled boards, written as the picture you actually see on the watch.
//
//   H  the hero, the 2x2 block that has to get out
//   G  a guard, lying down across two cells
//   V  a general, standing up across two cells
//   S  a soldier, one cell
//   .  a free cell
//
// Every board is the classic 4x5 frame with the exit under the two middle cells
// of the bottom row, so the hero is out once its top left corner reaches (1, 3).
// `par` is the shortest possible game in single-cell moves; a test recomputes
// each one with a breadth-first search, so a mistyped board or a wrong par fails
// the build rather than shipping.
//
// A board is known by its number and nothing else. The list is ordered easiest
// first, so the number IS the difficulty - which a name like "Sentries" never
// managed to say - and it needs no translating in any of the eleven languages.
// A board added later simply takes the next number, and its record starts empty
// like any other. The number is also the storage key for that board's record, so
// boards must only ever be appended, never reordered or inserted between: that
// would hand one board's record to another.

/** Where the hero has to reach: the cell in front of the gate. */
val GOAL = Cell(1, 3)

const val BOARD_COLS = 4
const val BOARD_ROWS = 5

/** A cell of the grid, counted from the top left. */
data class Cell(
    val x: Int,
    val y: Int,
)

/** One board: its number, its shortest possible game, and where its blocks start. */
data class Level(
    val id: Int,
    val par: Int,
    val cols: Int,
    val rows: Int,
    val goal: Cell,
    val blocks: List<Block>,
)

private val CHARS = Kind.entries.associateBy { it.mark }

private val LAYOUTS =
    listOf(
        7 to listOf("VHHV", "VHHV", "VSSV", "V..V", "...."),
        17 to listOf("VHHV", "VHHV", "VSSV", "VSSV", "...."),
        26 to listOf("VHHV", "VHHV", "VGGV", "VS.V", "..S."),
        51 to listOf("VHHV", "VHHV", "VGGV", "VSSV", "S..."),
        92 to listOf("SHHS", "VHHV", "VGGV", "VSSV", "V..V"),
        116 to listOf("VHHV", "VHHV", "VGGV", "VSSV", "S..S"),
    )

/**
 * Read a picture into blocks.
 *
 * A block is claimed at its top left cell, and every other cell it should cover
 * has to carry the same character - so a typo in the art is an error rather than
 * a quietly different board.
 */
fun parseArt(art: List<String>): Triple<Int, Int, List<Block>> {
    require(art.isNotEmpty() && art[0].isNotEmpty()) { "a board needs at least one cell" }
    val rows = art.size
    val cols = art[0].length
    val taken = Array(rows) { BooleanArray(cols) }
    val blocks = mutableListOf<Block>()

    for (y in 0 until rows) {
        require(art[y].length == cols) { "row $y is ${art[y].length} cells wide, expected $cols" }
        for (x in 0 until cols) {
            val char = art[y][x]
            if (char == '.' || taken[y][x]) continue
            val kind = CHARS[char] ?: error("unknown block \"$char\" at $x,$y")
            require(x + kind.w <= cols && y + kind.h <= rows) {
                "$kind at $x,$y hangs off the board"
            }
            claim(art, taken, x, y, kind)
            blocks.add(Block(kind, x, y))
        }
    }
    return Triple(cols, rows, blocks)
}

/**
 * Mark every cell a block covers as spoken for, checking on the way that the art
 * really does draw the whole block. A cell that carries a different character is
 * a typo, and one that is already taken belongs to a block declared earlier.
 */
private fun claim(
    art: List<String>,
    taken: Array<BooleanArray>,
    x: Int,
    y: Int,
    kind: Kind,
) {
    val char = art[y][x]
    for (dy in 0 until kind.h) {
        for (dx in 0 until kind.w) {
            require(art[y + dy][x + dx] == char) { "$kind at $x,$y is not a full block" }
            taken[y + dy][x + dx] = true
        }
    }
}

/** Ordered easiest to hardest; the menus walk this list. */
val LEVELS: List<Level> =
    LAYOUTS.mapIndexed { index, (par, art) ->
        val (cols, rows, blocks) = parseArt(art)
        Level(id = index + 1, par = par, cols = cols, rows = rows, goal = GOAL, blocks = blocks)
    }

val FIRST_LEVEL: Int = LEVELS.first().id

fun levelIndex(id: Int): Int = LEVELS.indexOfFirst { it.id == id }

/**
 * The board with this number, or the first one when the number is unknown - a
 * stored choice from an older version must not leave the game with nothing to
 * play.
 */
fun levelById(id: Int): Level = LEVELS.getOrElse(levelIndex(id)) { LEVELS.first() }

/** The ladder is a loop, so the menus can walk it in either direction forever. */
fun nextLevel(id: Int): Level {
    val index = levelIndex(id)
    return LEVELS[(if (index == -1) 0 else index + 1) % LEVELS.size]
}

fun previousLevel(id: Int): Level {
    val index = levelIndex(id)
    val from = if (index == -1) 0 else index
    return LEVELS[(from - 1 + LEVELS.size) % LEVELS.size]
}
