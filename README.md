# Skedpojkar

A RuneLite plugin with a grab-bag of fun features: event announcements (chat + sound),
side-panel minigames, and party-based multiplayer tic-tac-toe.

Everything compiles and loads, but several parts are deliberately barebones —
see [Current state](#current-state-whats-real-whats-barebones) before judging it. :)

## Features

### Announcements (sounds + chat messages)
Reacts to in-game events with a local chat message (only you see it) and/or a sound:
- Your level-ups and deaths (death picks randomly between two sound files)
- A **target player** appearing nearby, dying, or talking in public chat.
  Target usernames are typed into the plugin settings (comma-separated), so no
  names are ever hardcoded in this public repo.
- PvP: hitting a 0 on another player (30% chance), and killing a player
  (a player you recently damaged dies; sound plays 2 s later)
- Completing Floor 5 of the Hallowed Sepulchre or the Corrupted Gauntlet
- Passing the Al Kharid toll gate

Sounds are uncompressed PCM `.wav` files (a renamed `.mp3` won't play) you drop
into `~/.runelite/skedpojkar-sounds/`
(on Windows: `C:\Users\<you>\.runelite\skedpojkar-sounds\`). The folder and a
`README.txt` listing the expected file names are created automatically on first start.

### Side-panel minigames
A sidebar button (orange "S" icon) opens a panel with tabs:
- **Cookies** — cookie clicker; the count persists between sessions
- **TTT** — tic-tac-toe against a simple AI (win > block > center > random)
- **Facts** — random facts from a built-in pool
- **Party** — multiplayer tic-tac-toe (see below)

### Party tic-tac-toe
Both players run this plugin and join the same party via RuneLite's built-in
**Party plugin** (same passphrase). Moves are relayed over RuneLite's official
party websocket. The member with the lowest party id is X and moves first,
turns alternate, and wins/draws are detected. Designed for exactly 2 players.

## Current state: what's real, what's barebones

| Area | Status |
|---|---|
| Event triggers (level-up, death, targets, PvP, Sepulchre, Gauntlet, Al Kharid gate) | Working; verified in a live client except Sepulchre/Gauntlet completions (message matching in place, wordings confirmed) |
| Chat announcements | Working, verified in-game |
| Sound playback | Working, verified in-game — but **no sound files ship with the plugin**; silent until you drop PCM `.wav`s in the sounds folder. Missing files are skipped with only a debug log; unsupported formats log an explanation. |
| Cookie clicker | Working; count persists per character |
| Tic-tac-toe vs AI | Fully working, verified in-game |
| Facts | Working; pool is a hardcoded array |
| Party tic-tac-toe | Working: consistent X/O (lowest party id is X), enforced turns, win/draw detection. Designed for exactly 2 players. |
| Sidebar icon | Placeholder drawn in code, not a real image |
| In-game testing | Core features verified in a live client; not yet verified: Sepulchre/Gauntlet triggers, party TTT rule changes (turns/wins) |

## How to test (once you can run the client)

Prerequisites (already verified on this machine): JDK 11+, VS Code with the
**Extension Pack for Java** installed.

1. Open [SkedpojkarPluginTest.java](src/test/java/com/skedpojkar/SkedpojkarPluginTest.java)
   and click **Run** above its `main` method. A real RuneLite client opens with the
   plugin loaded. (Terminal alternative: `./gradlew build` only verifies compilation.)
2. Log in, then work through this checklist:
   - [ ] Orange "S" button appears in the RuneLite sidebar; panel opens with 4 tabs
   - [ ] Plugin appears in RuneLite settings; toggles and the target-players field save
   - [ ] Cookie count survives closing and reopening the client
   - [ ] TTT tab: AI plays back, win/draw is detected, New game resets
   - [ ] Gain a level (or use a lamp) → chat message appears
   - [ ] `~/.runelite/skedpojkar-sounds/` exists with its README.txt; drop any
         `.wav` in as `level_up.wav` and level up again → sound plays
   - [ ] Add a friend's RSN to target players; have them walk into view → message
   - [ ] Multiplayer: both of you install the plugin and join the same party
         (Party plugin → same passphrase) → moves appear on both screens
         (small delay is normal: moves apply when the server echoes them back)

If something misbehaves, logs are in `~/.runelite/logs/client.log` — the plugin
logs under `com.skedpojkar`.

## Architecture / where to change things

```
src/main/java/com/skedpojkar/
├── SkedpojkarPlugin.java      # Entry point: wires everything up in startUp()/shutDown()
├── SkedpojkarConfig.java      # Settings panel definition (each method = one setting)
├── announce/
│   └── AnnouncementTriggers.java # Feature 1: @Subscribe event handlers → chat/sound
├── sound/
│   ├── Sound.java                # Enum of sounds → expected .wav file names
│   └── SoundEngine.java          # Folder setup + .wav playback with volume
├── multiplayer/
│   ├── PartyGameMessage.java     # The message relayed between party members
│   └── PartyTicTacToe.java       # Shared board state + party send/receive
└── panel/
    ├── SkedpojkarPanel.java   # The sidebar panel (tab container)
    └── *Panel.java               # One class per tab, plain Java Swing
```

### Recipes for common extensions

**Add a new announcement trigger:** add an `@Subscribe` method to
`AnnouncementTriggers` for any [RuneLite event](https://static.runelite.net/api/runelite-api/net/runelite/api/events/package-summary.html)
(e.g. `ItemContainerChanged`, `AnimationChanged`, `InteractingChanged`), call
`announce(...)` and/or `soundEngine.play(...)`, and add a toggle to the config
interface. Quest completions / collection log slots are detected by regex on chat
messages — see [c-engineer-completed](https://github.com/m0bilebtw/c-engineer-completed)
for ready-made patterns.

**Add a new sound:** add one line to the `Sound` enum with its file name;
the sounds-folder README updates itself on next startup.

**Add a new minigame tab:** create a `JPanel` subclass in `panel/` and add one
`tabs.addTab(...)` line in `SkedpojkarPanel`. Persist state via `ConfigManager`
like `CookieClickerPanel` does. Note: only touch Swing from the Swing thread —
if reacting to game/party events, wrap UI updates in `SwingUtilities.invokeLater`
(see `PartyTicTacToe.notifyBoardChanged`).

**Improve party tic-tac-toe:** turn enforcement, X/O assignment and win/draw
detection are done. Remaining ideas: a challenge/accept handshake instead of an
always-open board, and handling 3+ party members gracefully. New message types:
extend `PartyMemberMessage`, register in the plugin's `startUp()` with
`wsClient.registerMessage(...)`, receive via an `@Subscribe on<ClassName>` method.

**Replace the placeholder icon:** put a 16x16ish `icon.png` under
`src/main/resources/com/skedpojkar/`, load it with
`ImageUtil.loadImageResource(SkedpojkarPlugin.class, "icon.png")`, and delete
`createIcon()`. A root-level `icon.png` (max 48x72) is also required for Plugin Hub
submission.

**Grow the facts pool:** edit the array in `FactsPanel`, or move it to a bundled
resource file read via `getResource