# Corgi's Various Features

A RuneLite plugin with a grab-bag of fun features: event announcements (chat + sound),
side-panel minigames, and party-based multiplayer tic-tac-toe.

Everything compiles and loads, but several parts are deliberately barebones —
see [Current state](#current-state-whats-real-whats-barebones) before judging it. :)

## Features

### Announcements (sounds + chat messages)
Reacts to in-game events with a local chat message (only you see it) and/or a sound:
- Your level-ups and deaths
- A **target player** appearing nearby, dying, or talking in public chat.
  Target usernames are typed into the plugin settings (comma-separated), so no
  names are ever hardcoded in this public repo.

Sounds are `.wav` files you drop into `~/.runelite/corgi-features-sounds/`
(on Windows: `C:\Users\<you>\.runelite\corgi-features-sounds\`). The folder and a
`README.txt` listing the expected file names are created automatically on first start.

### Side-panel minigames
A sidebar button (orange "C" icon) opens a panel with tabs:
- **Cookies** — cookie clicker; the count persists between sessions
- **TTT** — tic-tac-toe against a simple AI (win > block > center > random)
- **Facts** — random facts from a built-in pool
- **Party** — multiplayer tic-tac-toe (prototype, see below)

### Party tic-tac-toe (prototype)
Both players run this plugin and join the same party via RuneLite's built-in
**Party plugin** (same passphrase). Moves are relayed over RuneLite's official
party websocket: your moves show as X, party members' as O.

## Current state: what's real, what's barebones

| Area | Status |
|---|---|
| Event triggers (level-up, death, target spawn/death/chat) | Working, but trigger set is small |
| Chat announcements | Working |
| Sound playback | Working code, but **no sound files ship with the plugin** — silent until you drop `.wav`s in the sounds folder. Missing files are skipped with only a debug log. |
| Cookie clicker | Working + persistent; no upgrades/auto-clickers yet |
| Tic-tac-toe vs AI | Fully working |
| Facts | Working; pool is a hardcoded array |
| Party tic-tac-toe | Prototype: no turn enforcement, no win detection, both players see *themselves* as X, only sensible with exactly 2 party members |
| Sidebar icon | Placeholder drawn in code, not a real image |
| In-game testing | **None yet** — compiles against the RuneLite API but has never been run in a live client |

## How to test (once you can run the client)

Prerequisites (already verified on this machine): JDK 11+, VS Code with the
**Extension Pack for Java** installed.

1. Open [CorgiFeaturesPluginTest.java](src/test/java/com/corgifeatures/CorgiFeaturesPluginTest.java)
   and click **Run** above its `main` method. A real RuneLite client opens with the
   plugin loaded. (Terminal alternative: `./gradlew build` only verifies compilation.)
2. Log in, then work through this checklist:
   - [ ] Orange "C" button appears in the RuneLite sidebar; panel opens with 4 tabs
   - [ ] Plugin appears in RuneLite settings; toggles and the target-players field save
   - [ ] Cookie count survives closing and reopening the client
   - [ ] TTT tab: AI plays back, win/draw is detected, New game resets
   - [ ] Gain a level (or use a lamp) → chat message appears
   - [ ] `~/.runelite/corgi-features-sounds/` exists with its README.txt; drop any
         `.wav` in as `level_up.wav` and level up again → sound plays
   - [ ] Add a friend's RSN to target players; have them walk into view → message
   - [ ] Multiplayer: both of you install the plugin and join the same party
         (Party plugin → same passphrase) → moves appear on both screens
         (small delay is normal: moves apply when the server echoes them back)

If something misbehaves, logs are in `~/.runelite/logs/client.log` — the plugin
logs under `com.corgifeatures`.

## Architecture / where to change things

```
src/main/java/com/corgifeatures/
├── CorgiFeaturesPlugin.java      # Entry point: wires everything up in startUp()/shutDown()
├── CorgiFeaturesConfig.java      # Settings panel definition (each method = one setting)
├── announce/
│   └── AnnouncementTriggers.java # Feature 1: @Subscribe event handlers → chat/sound
├── sound/
│   ├── Sound.java                # Enum of sounds → expected .wav file names
│   └── SoundEngine.java          # Folder setup + .wav playback with volume
├── multiplayer/
│   ├── PartyGameMessage.java     # The message relayed between party members
│   └── PartyTicTacToe.java       # Shared board state + party send/receive
└── panel/
    ├── CorgiFeaturesPanel.java   # The sidebar panel (tab container)
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
`tabs.addTab(...)` line in `CorgiFeaturesPanel`. Persist state via `ConfigManager`
like `CookieClickerPanel` does. Note: only touch Swing from the Swing thread —
if reacting to game/party events, wrap UI updates in `SwingUtilities.invokeLater`
(see `PartyTicTacToe.notifyBoardChanged`).

**Improve party tic-tac-toe:** the known gaps, roughly in order of value —
turn enforcement and consistent X/O assignment (e.g. lower `memberId` is X and
moves alternate), win/draw detection (reuse `LINES` logic from `TicTacToePanel`),
and a challenge/accept handshake instead of a free-for-all board. New message
types: extend `PartyMemberMessage`, register in the plugin's `startUp()` with
`wsClient.registerMessage(...)`, receive via an `@Subscribe on<ClassName>` method.

**Replace the placeholder icon:** put a 16x16ish `icon.png` under
`src/main/resources/com/corgifeatures/`, load it with
`ImageUtil.loadImageResource(CorgiFeaturesPlugin.class, "icon.png")`, and delete
`createIcon()`. A root-level `icon.png` (max 48x72) is also required for Plugin Hub
submission.

**Grow the facts pool:** edit the array in `FactsPanel`, or move it to a bundled
resource file read via `getResourceAsStream`.

## Rules note

This plugin only reads game events, plays local sounds, prints local-only chat
messages, and draws UI. It performs no input automation and never acts on the
game — keep it that way for Plugin Hub eligibility.

## Path to the Plugin Hub

1. Test everything above in a live client
2. Real icon, `LICENSE` file (BSD 2-Clause — required by the hub)
3. Push to a public GitHub repo
4. PR to [runelite/plugin-hub](https://github.com/runelite/plugin-hub) adding a
   manifest with the repo URL + commit hash
