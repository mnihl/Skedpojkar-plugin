# Skedpojkar

Fun sounds and minigames for you and your clanmates: sound/chat announcements
for in-game events, side-panel minigames, and party-based multiplayer
tic-tac-toe.

## Installation

Install RuneLite, open the Configuration sidebar (wrench icon), click
**Plugin Hub** at the top of the plugin list, and search for **Skedpojkar**.

## Features

### Announcements (sounds + chat messages)

Reacts to in-game events with a sound and/or a local chat message (only you
see it). Every trigger has its own toggle in the plugin settings:

| Trigger | Sound file | Default |
|---|---|---|
| You level up | `level_up.wav` | on |
| You die | `own_death_1.wav` / `own_death_2.wav` (random pick) | on |
| Clan broadcast: a member's PvP kill | `clan_kill.wav` | on |
| Clan broadcast: a member died | `clan_death.wav` | on |
| Clan broadcast: a member got a drop | `clan_drop.wav` | on |
| Anyone talks in your clan/friends channel | `clan_chat.wav` | off (noisy) |
| You hit a 0 on another player (30% chance) | `pvp_zero_hit.wav` | on |
| You kill a player (plays 2 s after) | `pvp_kill.wav` | on |
| You complete Floor 5 of the Hallowed Sepulchre | `sepulchre_floor_5.wav` | on |
| You pass the Al Kharid toll gate | `good_job.wav` | on |
| You complete the Corrupted Gauntlet | `good_job.wav` | on |
| You pass the gate having completed Prince Ali Rescue | chat message | on |
| A secret phrase sequence arrives via PMs | `pm_sequence.wav` + chat message | on |
| You enter a house lacking an ornate pool / jewellery box | chat message | on |
| Your opened bank is worth under a threshold (default 1M) | chat message, once per session | on |

Level-up and your own death also print a chat message; the rest are sound-only.
Clan triggers match the *type* of broadcast, never specific player names.
Global settings: chat messages on/off, sounds on/off, volume (default 25).

**Custom sounds:** all sounds are bundled and work out of the box. To replace
one (or add one for a trigger that ships without a sound — currently
`level_up`, `clan_kill`, `clan_death` and `clan_drop`), drop a `.wav` file with
that name into `~/.runelite/skedpojkar-sounds/` (Windows:
`C:\Users\<you>\.runelite\skedpojkar-sounds\`). Your files always take
precedence over the bundled ones. Files must be uncompressed PCM `.wav` — a
renamed `.mp3` will not play. The folder and a `README.txt` listing the file
names are created automatically on first start.

### Side-panel minigames

A sidebar button (orange spoon) opens a panel with tabs:

- **Runes** — runecrafting clicker: click to craft, buy RS-flavored upgrades
  (pouches, essence miners, ZMI trips...), prestige through the real rune
  tiers Air → Wrath (each doubles your clicks), then Ascend the Runespan for
  a permanent multiplier. Progress saves per character; old cookie-clicker
  counts migrate into starting points. No offline gains — upgrades only work
  while you're logged in. See [RUNECLICKER_DESIGN.md](RUNECLICKER_DESIGN.md).
- **TTT** — tic-tac-toe against a simple AI (win > block > center > random)
- **Facts** — random facts from a built-in pool
- **Party** — multiplayer tic-tac-toe (see below)

### Party tic-tac-toe

Play tic-tac-toe with a friend through the game client: both players install
this plugin and join the same party via RuneLite's built-in **Party plugin**
(same passphrase). The member with the lowest party id is X and moves first,
turns alternate, and wins/draws are detected. Designed for exactly 2 players.
Moves are relayed over RuneLite's official party websocket; a short delay
before your own move appears is normal.

## Bugs & feature requests

Found a bug or want a feature? Open an issue on
[GitHub](https://github.com/mnihl/Skedpojkar-plugin/issues). For bug reports,
please include what happened, what you expected, and — if the client was open —
the relevant lines from `~/.runelite/logs/client.log` (the plugin logs under
`com.skedpojkar`). For Runeclicker problems, mention which character you were
on and whether you play on multiple computers.

## Privacy & rules

This plugin only reads game events, plays local sounds, prints local-only chat
messages, and draws UI. It performs no input automation and never acts on the
game. No triggers key on specific player names — clan announcements react to
broadcast types only, in channels you are a member of.

---

## Development

Prerequisites: JDK 11+ and an IDE with Java support (developed with VS Code +
Extension Pack for Java; IntelliJ works too).

Run [SkedpojkarPluginTest.java](src/test/java/com/skedpojkar/SkedpojkarPluginTest.java)'s
`main` method (VM option `-ea` required) to boot a RuneLite client with the
plugin loaded. `./gradlew build` verifies compilation without launching.
Logs are in `~/.runelite/logs/client.log`, under `com.skedpojkar`.

### Known gaps / not yet verified in-game

- Sepulchre floor 5 and Corrupted Gauntlet triggers (wordings confirmed against
  real chat messages, firing not yet observed)
- Clan broadcast phrases are best-guess — verify against real broadcasts
- Party TTT turn/win rules with two clients
- Bundled-sound fallback (playing from the jar rather than the sounds folder)
- POH object names ("Exit portal", "Ornate pool ...", "Ornate jewellery box")
  are best-guess — verify by entering a house
- Bank value uses GE prices (untradeables count as 0); placeholder messages in
  the PM-sequence, house, and bank triggers still need their real jokes

### Architecture

```
src/main/java/com/skedpojkar/
├── SkedpojkarPlugin.java         # Entry point: wires everything up in startUp()/shutDown()
├── SkedpojkarConfig.java         # Settings panel definition (each method = one setting)
├── announce/
│   └── AnnouncementTriggers.java # @Subscribe event handlers → chat/sound
├── sound/
│   ├── Sound.java                # Enum of sounds → expected .wav file names
│   └── SoundEngine.java          # Bundled + user sounds, playback via AudioPlayer
├── multiplayer/
│   ├── PartyGameMessage.java     # The message relayed between party members
│   └── PartyTicTacToe.java       # Shared board state + game rules + party send/receive
└── panel/
    ├── SkedpojkarPanel.java      # The sidebar panel (tab container)
    └── *Panel.java               # One class per tab, plain Java Swing
```

### Recipes for common extensions

**Add a new announcement trigger:** add an `@Subscribe` method to
`AnnouncementTriggers` for any [RuneLite event](https://static.runelite.net/api/runelite-api/net/runelite/api/events/package-summary.html),
call `announce(...)` and/or `soundEngine.play(...)`, and add a toggle to the
config interface. Quest completions / collection log slots are detected by
regex on chat messages — see [c-engineer-completed](https://github.com/m0bilebtw/c-engineer-completed)
for ready-made patterns. Player-name-targeted triggers are not allowed
(see [Rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)).

**Add a new sound:** add one line to the `Sound` enum; bundle a default `.wav`
in `src/main/resources/com/skedpojkar/sound/` if it should ship with one. The
sounds-folder README updates itself on next startup.

**Add a new minigame tab:** create a `JPanel` subclass in `panel/` and add one
`tabs.addTab(...)` line in `SkedpojkarPanel`. Persist state via `ConfigManager`
like `RuneClickerPanel` does. Only touch Swing from the Swing thread — if
reacting to game/party events, wrap UI updates in `SwingUtilities.invokeLater`
(see `PartyTicTacToe.notifyBoardChanged`).

**Improve party tic-tac-toe:** remaining ideas — a challenge/accept handshake
instead of an always-open board, and handling 3+ party members gracefully.
New message types: extend `PartyMemberMessage`, register in the plugin's
`startUp()` with `wsClient.registerMessage(...)`, receive via an
`@Subscribe on<ClassName>` method.

**Change the icon:** the spoon is `icon.svg` at the repo root; regenerate the
PNGs from it (16x16 to `src/main/resources/com/skedpojkar/icon.png` for the
sidebar, 48x48 root-level `icon.png` for the Plugin Hub listing).

**Grow the facts pool:** edit the array in `FactsPanel`, or move it to a
bundled resource file read via `getResourceAsStream`.

### Releasing an update

Push to this repo, then open a PR to
[runelite/plugin-hub](https://github.com/runelite/plugin-hub) updating the
`commit=` hash in `plugins/skedpojkar` to the new commit.
