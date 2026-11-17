# Puzzle Buddy Android

| | |
|---|---|
| applicationId | `com.jacobrozell.puzzlebuddy` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Assemble | `./gradlew assembleDebug` |
| iOS behavior | `../PuzzleBuddy` (specs are canonical) |
| Scoreboard | `PARITY.md` |
| Device | One booted phone AVD for UI checks. Galaxy release pass: `DaRules/docs/samsung-device.md` |

UI checks use the `android-verify` skill. Do not copy those steps here. A release install on the Galaxy uses the debug keystore locally. Do not commit that `signingConfig` line.
