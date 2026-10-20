# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android Tic-Tac-Toe ("Triqui") app for a UNAL mobile development course, built incrementally per assignment ("retoN" branches/commits). Single module `app`, package `co.edu.unal.tictactoe`, Jetpack Compose UI (Material3), Kotlin + one Java class, Firebase Realtime Database for online play. minSdk 24, compile/target SDK 37, Java 11. Dependency versions live in `gradle/libs.versions.toml`. Code comments and UI strings are in Spanish.

## Commands

Use `gradlew.bat` on Windows (or `./gradlew` from Git Bash):

- Build debug APK: `./gradlew assembleDebug`
- Install on device/emulator: `./gradlew installDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Single unit test: `./gradlew testDebugUnitTest --tests "co.edu.unal.tictactoe.ExampleUnitTest"`
- Instrumented tests (needs device): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lintDebug`

Firebase requires `app/google-services.json` (google-services plugin).

## Architecture

- **`TicTacToeGame.java`** — pure game engine: board as `char[9]`, `HUMAN_PLAYER='X'`, `COMPUTER_PLAYER='O'`, `DifficultyLevel` (Easy/Harder/Expert) driving `getComputerMove()`. `checkForWinner()` returns 0 = no winner yet, 1 = tie, 2 = X wins, 3 = O wins. This encoding is reused by online mode (`OnlineGame.winner`).
- **`AndroidTicTacToeActivity.kt`** — `MainActivity` plus most UI. Navigation is a manual string state (`screen` = `"menu" | "single" | "lobby" | "online"`) in `setContent` with `BackHandler`s — no Navigation library. `TicTacToeBoard` holds single-player state (scores, difficulty, sounds via SoundPool/MediaPlayer, computer move delay); `GameBoardCanvas` draws the board with Compose Canvas.
- **Online mode**: `LobbyScreen.kt` (list/create/join waiting games, connection banner) → `OnlineGameScreen.kt` (play a game). Both talk only to **`OnlineRepository`** (singleton object), which wraps Firebase:
  - Games stored under `games/<id>` as `OnlineGame` (board is a 9-char string, `turn`, `status` = waiting/playing/finished/abandoned). Waiting games older than 10 min are treated as abandoned.
  - `joinGame` and `makeMove` use Firebase **transactions** to avoid races; `listen*` functions return an unsubscribe lambda to call from `DisposableEffect`.
  - Player identity is a UUID persisted in SharedPreferences.
- **Persistence**: all local state (scores, difficulty, player name/id) uses SharedPreferences file `"ttt_prefs"`, shared across files.
