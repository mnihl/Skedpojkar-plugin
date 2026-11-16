# CLAUDE.md

Orientation for coding agents. For the feature list and architecture breakdown,
read [README.md](README.md); for Runeclicker's mechanics and balance numbers, read
[RUNECLICKER_DESIGN.md](RUNECLICKER_DESIGN.md). This file only captures the
decisions and constraints that aren't discoverable from the code.

## What this is

A RuneLite plugin ("Skedpojkar"), at version 1.0, written for the public RuneLite
Plugin Hub. Three features, all implemented: event announcements (chat + sound),
side-panel minigames (dominated by Runeclicker, an idle clicker that is about half
the codebase), and party-based multiplayer tic-tac-toe. The README's "Known gaps"
section lists what has not been confirmed in a live client.

## Owner context

- Not deeply experienced in Java or Gradle — explain Java/build concepts, don't assume them.
- Uses **VS Code** with the Extension Pack for Java (not IntelliJ). Note: the VS Code
  Java extension emits compiled `.class` files into `bin/` (gitignored); Gradle uses
  `build/`. Neither is source.

## Design decisions — do not silently undo these

- **Public Hub, not sideloading.** Code is written to be publishable: open, generic,
  reusable. Hub rules apply, e.g. sound goes through RuneLite's `AudioPlayer`, not
  `javax.sound`.
- **No player-name targeting, anywhere.** Triggers key on event or message *type*
  only, never on who is involved — not as constants and not as a config field
  either (an earlier design used a comma-separated config field; it was removed
  because RuneLite rejects player-targeted features). The friends this is built
  for must also not be identifiable from the public repo. Sounds/jokes in the repo
  are fine to be public; names are not.
- **No input automation, ever.** The plugin only reads events, plays local sounds,
  prints local-only chat messages, and draws UI. It must never click, move the player,
  send real chat, or otherwise act on the game — this is required for Hub eligibility
  and to avoid any botting-detection risk. Reject/redesign any feature that crosses this.
- **No offline or logged-out progress in Runeclicker.** Income only accrues while
  logged in. The save/load guards in `RuneClickerPanel` (profile-key checks, keeping
  in-memory state on a missing read) exist because an early build wiped progress
  during login; don't simplify them away.
- **Runeclicker saves are versioned.** State is one pipe-delimited string per
  character (`STATE_VERSION` in `RuneClickerGame`). Any new persisted field needs a
  version bump, a default for older saves, and a test loading an old-version
  string, so existing players keep their progress.
- **Rules stay out of the UI.** Game rules and text matching live in plain classes
  with no Swing or RuneLite in them (`clicker/RuneClickerGame`,
  `multiplayer/TicTacToeRules`, `announce/ClanBroadcast`, `announce/PmSequence`) so
  they can be unit-tested. Panels and event handlers only draw and delegate. Time
  and randomness are passed in, never read from the clock inside the rules.

## Build & test

- `./gradlew build` — compiles against the real RuneLite API and runs the unit
  tests in `src/test`. This is the verify-without-a-client check; use it after
  changes. Add or update tests when changing rules.
- **Balance changes:** run `RuneClickerSimulation` (its `main` prints time per
  prestige tier for a simulated player) before and after touching Runeclicker
  numbers, instead of guessing.
- **Live client testing:** run the `main` method in
  [SkedpojkarPluginTest.java](src/test/java/com/skedpojkar/SkedpojkarPluginTest.java)
  from VS Code (Run button above `main`, VM option `-ea`), which boots RuneLite
  with the plugin loaded. Anything not confirmed in-game should be treated as
  untested.
- The tests cover rules only. UI, sounds and the event triggers themselves are
  not covered and still need the live client.

## Repo notes

- The Gradle wrapper's `distributionSha256Sum` was previously wrong and has been
  corrected to the official Gradle 8.9 checksum; don't "fix" it back.
- Releasing: push here, then open a PR to `runelite/plugin-hub` updating the
  `commit=` hash in `plugins/skedpojkar`.
