# CLAUDE.md

Orientation for coding agents. For the detailed feature/architecture breakdown,
read [README.md](README.md) — this file only captures the decisions and constraints
that aren't discoverable from the code.

## What this is

A RuneLite plugin ("Corgi's Various Features") the owner is building to eventually
publish to the public RuneLite Plugin Hub. Three features, all in an early but
compiling/loading state: event announcements (chat + sound), side-panel minigames,
and party-based multiplayer tic-tac-toe. See the README's "Current state" table for
what is real vs. barebones.

## Owner context

- Not deeply experienced in Java or Gradle — explain Java/build concepts, don't assume them.
- Uses **VS Code** with the Extension Pack for Java (not IntelliJ). Note: the VS Code
  Java extension emits compiled `.class` files into `bin/` (gitignored); Gradle uses
  `build/`. Neither is source.

## Design decisions — do not silently undo these

- **Public Hub, not sideloading.** Code is written to be publishable: open, generic,
  reusable.
- **No hardcoded usernames.** The friends this is built for must not be identifiable
  from the public repo. Target players come from a config field (comma-separated),
  never constants in code. Sounds/jokes in the repo are fine to be public; names are not.
- **No input automation, ever.** The plugin only reads events, plays local sounds,
  prints local-only chat messages, and draws UI. It must never click, move the player,
  send real chat, or otherwise act on the game — this is required for Hub eligibility
  and to avoid any botting-detection risk. Reject/redesign any feature that crosses this.

## Build & test

- `./gradlew build` — compiles against the real RuneLite API. This is the
  verify-without-a-client check; use it after changes.
- **Live client testing is now available.** The owner can run the plugin in a real
  RuneLite client: run the `main` method in
  [CorgiFeaturesPluginTest.java](src/test/java/com/corgifeatures/CorgiFeaturesPluginTest.java)
  from VS Code (Run button above `main`), which boots RuneLite with the plugin loaded.
  Earlier work predated this, so anything not yet confirmed in-game should be treated
  as untested — see the checklist in the README when validating behavior.

## Repo notes

- The Gradle wrapper's `distributionSha256Sum` was previously wrong and has been
  corrected to the official Gradle 8.9 checksum; don't "fix" it back.
