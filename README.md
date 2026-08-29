# WearOS Klotski

**Block Escape** - Klotski, the sliding block puzzle also known as Huarong Pass -
for **Wear OS** watches, in Kotlin and Jetpack Compose. Ten blocks are packed into
a 4x5 tray with barely any room to move; slide them, without lifting or turning any
of them, until the big 2x2 commander can leave through the gate at the bottom.
Everything runs on the watch: no phone, no network, no account.

This is a port of [AmazfitKlotski](https://github.com/dchernykh1984/AmazfitKlotski),
the same game as a Zepp OS mini app. The rules, the six boards, the layout
proportions, the block portraits and the eleven translations are carried over
unchanged; the implementation is new.

## Playing it

- **Board** - the classic 4x5 tray, scaled to the round screen and centred, with
  the move counter in the cap above it and the controls in the margins beside and
  below it. The counter is a plain count of the moves you have spent; there is no
  allowance to run out of and no way to lose.
- **Controls** - tap a block to pick it up, then swipe up / down / left / right to
  slide it one cell. The selected block keeps the gold ring, so a run of swipes
  pushes the same block along; tap another block to move the ring to it.
- **Undo, restart, menu** - undo takes back one move (and the counter with it),
  restart puts the board back as it started, and the menu pauses over the board so
  the position is still there when you come back. **Back** during a game opens that
  menu rather than leaving, so sliding a block right cannot cost you the session.
- **Six boards, numbered 1 to 6** - a ladder from a seven-move warm-up to the
  classic Huarong Pass, which cannot be solved in fewer than 116 moves. The number
  is the whole name: the list is ordered by difficulty, so the number says more
  than any name could, and it needs no translating. The board you played last is
  the one that opens next time.
- **Records** - one per board, shown on their own screen a board at a time, paged
  with a swipe. Each shows the moves and the clock of your best game and the fewest
  moves the board can possibly be solved in. Fewer moves is what makes a record;
  the clock only separates two games that took the same number of moves.
- **Languages** - English, Russian, German, French, Italian, Spanish, Portuguese,
  Dutch, Polish, Czech and Kazakh. The watch's own language is followed, and all
  eleven are offered individually in the system per-app language list - so Kazakh,
  which Zepp OS had no device-language code for and could never select, finally
  reaches the people it was translated for.

Every board's `par` is checked by a test that searches for the shortest possible
game and compares it, so the "minimum" the records screen shows is a proved number
rather than a remembered one.

## Devices

Round watches, **Wear OS 3 (API 30) and newer**. Built and tested against a
**OnePlus Watch 2R** (466x466 round, Wear OS 5). Every measurement is drawn for one
design and scaled to the diameter of the screen it lands on, so any round watch
gets the same board with correctly sized cells.

## Artwork

The ten block faces are details of a Qing dynasty album of Peking opera characters,
released under **CC0** by the Metropolitan Museum of Art. The files are the Zepp OS
app's, byte for byte. Provenance and per-file credits are in [docs/ASSETS.md](docs/ASSETS.md).

## Setup

```bash
git clone https://github.com/dchernykh1984/WearOSKlotski.git
cd WearOSKlotski
```

A JDK 17 and the Android SDK (compileSdk 36) are all that is needed; Gradle comes
with the repository through the wrapper. Point the build at your SDK with a
`local.properties` holding `sdk.dir=/path/to/Android/sdk`, or export `ANDROID_HOME`.

## Develop

```bash
./gradlew testDebugUnitTest   # the JVM unit tests
./gradlew koverVerify         # unit tests + the coverage floor
./gradlew ktlintCheck         # formatting
./gradlew detekt              # static analysis
./gradlew lintDebug           # Android Lint, including the Wear OS checks
./gradlew assembleDebug       # build the APK
./gradlew connectedDebugAndroidTest   # instrumented tests (needs a watch or emulator)
./gradlew installDebug        # install on a watch over ADB
```

The whole pull-request gate in one line, which is exactly what CI runs:

```bash
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest koverVerify assembleDebug assembleRelease
```

### Layout of the code

```
wear/
  src/main/AndroidManifest.xml         watch-only, standalone, no permissions
  src/main/java/com/dchernykh/klotski/
    MainActivity.kt                    the single activity
    KlotskiViewModel.kt                the state the screen draws
    game/Klotski.kt                    the rules: sliding, undo, solved
    game/Levels.kt                     the six boards, read from their pictures
    game/Clock.kt                      how long a game took, and how to write it
    game/Records.kt                    what counts as a record
    layout/RoundGeometry.kt            chord maths that keeps content off the bezel
    layout/ScreenLayout.kt             every box, scaled from the design
    layout/Tiles.kt                    where a block lands, and what a tap hit
    store/RecordStore.kt               the records, on Preferences DataStore
    ui/                                the Compose screens
  src/main/res/values*/strings.xml     the screen strings, a table per language
  src/main/res/drawable-nodpi/         the block portraits and the two controls
  src/test/                            JVM unit tests, including the par solver
  src/androidTest/                     instrumented tests - what needs a device
config/detekt/detekt.yml               static-analysis overrides
gradle/libs.versions.toml              every dependency and plugin version
```

The rule that shapes it: anything a test can reach without a device - the rules, the
boards, the clock, the record decision, the round-screen layout - is a plain Kotlin
class outside the Compose layer, and `koverVerify` holds it to a floor of 80. Only
what genuinely needs a device is exempt, and each exemption is written down where it
is made, with the instrumented test that covers it instead.

## Pre-commit hooks (contributors)

```bash
uv tool install pre-commit   # or: pipx install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg --hook-type pre-push
```

On commit: whitespace and line endings, YAML/TOML/XML well-formedness, a non-ASCII
guard on source and config (translations in `res/values-*/` are exempt - that is
what they are for), and a check that apostrophes in string resources are escaped,
which is an aapt2 error rather than a warning. On the commit message: Conventional
Commits. On push: ktlint, detekt and the unit tests.

## Continuous integration and releases

Every pull request must pass: pre-commit, `actionlint`, commitizen, the Gradle gate
above, a CodeQL analysis, an OSV dependency scan and the instrumented tests on two
Wear OS emulators.

Releases are automated with `release-please`: it maintains a version-bump PR from
the Conventional Commits and, when merged, tags a GitHub Release. The release build
then produces a **signed APK**, verifies its signature, records a build-provenance
attestation and attaches the APK and its R8 mapping file to the release.

Verify a published APK came from this repository:

```bash
gh attestation verify wearos-klotski-<version>.apk --repo dchernykh1984/WearOSKlotski
```

### Dependency locking

`wear/gradle.lockfile` pins every transitive version. After changing a dependency,
regenerate it with the **Update lockfiles** workflow (or
`./gradlew :wear:dependencies --write-locks`) and commit the result.

## License

Released under the [MIT License](LICENSE). The block artwork is CC0; see
[docs/ASSETS.md](docs/ASSETS.md).
