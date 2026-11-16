# Runeclicker — design & mechanics (v1.0)

Cookie-clicker, RS-modified: click a rune to craft, spend points on upgrades
that craft for you, and prestige up the rune tiers. Replaces the old cookie
clicker (existing cookie counts migrate into starting points, once, per
character). All numbers live as constants at the top of
`clicker/RuneClickerGame` (the rules, with no UI; `RuneClickerPanel` only draws
it) and are meant to be tuned. Run `RuneClickerSimulation` in `src/test` to see
the time per tier a change produces.

## Core loop

- The button is your current rune (real item sprite). Clicking crafts points;
  each click has a 5% chance to **crit** for ×10.
- Points buy **shop upgrades**: the talisman adds to points/click; the rest
  generate points/second (idle). Idle only accrues while logged in.
- At the **prestige cost** you reset points and upgrades, advance to the next
  rune, and permanently double *all* income (clicks and idle).
- **Golden runes** appear every 3–10 min for 10 s; catch one for a points
  jackpot or a ×7 income frenzy (30 s), 50/50.

## Income

`pointsPerClick = (1 + talismans) × 2^pouches × globalMultiplier`
`pointsPerSecond = Σ(building rate × count × milestones) × globalMultiplier + autoClicks`
`milestones = ×2 for each of 10 / 25 / 50 / 100 owned of that building (×16 at 100)`
`globalMultiplier = 2^tier × 3^attunement` (×2 during Daeyalt, ×7 during frenzy)

So each prestige doubles the whole economy — the same upgrades at the same
prices then produce double, making rebuilds fast. Prestige costs grow much
faster than that (see below), so the doubling speeds up the rebuild, not the
tier.

## Prestige tiers

Real Runecrafting unlock order, 15 tiers: Air, Mind, Water, Earth, Fire, Body,
Cosmic, Chaos, Astral, Nature, Law, Death, Blood, Soul, Wrath.

`cost = xpForLevel(PRESTIGE_LEVELS[tier]) × 4^tier × ASCENSION_COST_FACTOR^ascensions`

The base is the **real OSRS experience** for a level, via the authentic
`xpForLevel` formula; each tier has its own level in the `PRESTIGE_LEVELS`
table. The `× 4^tier` is there because income doubles every prestige while
shop prices stay flat, and bigger buildings unlock on the way up: without a
steep ramp the late tiers took seconds (in v1.0 a full ascension took under 7
minutes of clicking).

| Prestige | Level | Cost | Unlocks |
|---|---|---|---|
| Air → Mind | 14 | 2,107 | |
| Mind → Water | 24 | 28,112 | |
| Water → Earth | 31 | 237,328 | |
| Earth → Fire | 37 | 1,758,272 | Abyss runner |
| Fire → Body | 45 | 15,747,072 | |
| Body → Cosmic | 65 | 460,214,272 | |
| Cosmic → Chaos | 80 | 8,134,934,528 | Blood altar runner |
| Chaos → Astral | 100 | 235,784,765,440 | |
| Astral → Nature | 106 | 1,708,433,866,752 | |
| Nature → Law | 106 | 6,833,735,467,008 | Guardians of the Rift |
| Law → Death | 120 | 109,338,340,360,192 | |
| Death → Blood | 118 | 358,776,913,788,928 | |
| Blood → Soul | 115 | ~1.07 quadrillion | Runespan siphon |
| Soul → Wrath | 130 | ~18.8 quadrillion | |
| Wrath → Ascend | 132 | ~91.8 quadrillion | |

The levels are not strictly rising: a tier where a new building has just
unlocked needs a big jump in cost, the tiers after it less. The cost itself
always rises. Two levels are above 126, the highest (virtual) level in OSRS;
the experience formula simply continues past it.

From the fourth run on (three ascensions) the last costs pass what a 64-bit
whole number holds (~9.2 quintillion); they are still shown with full digits.

**Pacing.** The levels were fitted with `RuneClickerSimulation` so that, for a
player clicking once a second, each tier takes about 1.3× as long as the one
before: about 4 minutes for Air, about 2.7 hours for Wrath. Simulated time to
the first ascension (golden runes and Daeyalt ignored, so real play is a
little faster):

| Player | First ascension |
|---|---|
| Idle only | ~13.5 h |
| 1 click/s | ~11.5 h |
| 3 clicks/s | ~9.5 h |
| 6 clicks/s | ~8 h |

Clicking matters most in the early tiers. Later the buildings carry the
income, but the "Practised hands" synergy ties click value to building
income, so an active clicker stays ahead through the whole run.

To retune, run `RuneClickerSimulation` with the arguments `fit 2.7 1.3 1`
(hours for the Wrath tier, growth per tier, clicks per second): it prints a
suggested level table. Or change one level and rerun the pacing table; one
level is about 10% more cost. Refit after any change to buildings, synergies
or multipliers.

**After ascension** all prestige costs are multiplied by
`ASCENSION_COST_FACTOR^ascensions` (factor 5.0). It has to be well above
attunement's ×3 income per point, otherwise each ascension makes the next run
easier; at 5.0 the second run takes about an hour longer than the first.
Set the factor to 1.0 to disable.

History: ×10/tier (diverged) → ×3/tier → OSRS XP to level 99 (too fast) → OSRS
XP to level 126 + per-ascension scaling ×2 (v1.0; late tiers took seconds) →
fitted level table × 2^tier, ascension ×4 → four more buildings, tier unlocks
and ownership milestones; refitted table × 4^tier, ascension ×5 → synergies
(they cut a run from ~11 h to under 5 h on the old table); table refitted.

## Ascension (second prestige layer)

Past Wrath, the prestige button becomes **Ascend the Runespan**: everything
resets and you gain 1 Runespan point, spent on permanent perks (hidden until
your first ascension):

- **Wizard Finix's help** — auto-click 1/s per level (repeatable)
- **Pouch keeper** — pouches survive prestige
- **Perfect Ourania** — ZMI never fails
- **Golden magnetism** — golden runes appear twice as often
- **Runespan attunement** — ×3 all income, repeatable and stacking

## Shop upgrades

Repeatable cost = `baseCost × 1.15^owned`; reset on prestige. A plain click
on Buy buys one, Shift-click up to 10, Ctrl-click as many as the points allow.

The Upgrades, Synergies and Runespan perks sections fold away when their
header is clicked (all open on start; not saved). Numbers are always shown
with full digits (`NumberText`), however large.

| Upgrade | Effect | Base cost |
|---|---|---|
| <tier> talisman | +1 point/click | 50 |
| Rune essence miner | +0.5/s | 200 |
| Pure essence miner | +2/s | 1,500 |
| Abyssal leech | +8/s | 10,000 |
| ZMI altar trips | +30/s, 10% of seconds yield 0 | 75,000 |
| Wicked hood | +150/s | 400,000 |
| Abyss runner (from Fire) | +750/s | 2,400,000 |
| Blood altar runner (from Chaos) | +4,000/s | 15,000,000 |
| Guardians of the Rift (from Law) | +20,000/s | 90,000,000 |
| Runespan siphon (from Soul) | +100,000/s | 540,000,000 |

The four big buildings only appear in the shop from their rune tier on
(`UPGRADE_UNLOCK_TIERS`), so they are locked again after an ascension. The
shop shows a "??? — unlocks at X runes" line for the next one.

**Ownership milestones:** owning 10, 25, 50 and 100 of an idle building each
double that building's output (`MILESTONE_COUNTS`). Talismans have none.
Nothing extra is saved for this; it is derived from the counts.

**Synergies:** one-off purchases that make buildings boost each other. Each
building feeds the next bigger one: with the synergy owned, the target gets
+2% output for every one of the source building owned
(`SYNERGY_PERCENT_PER_SOURCE`). They reset on prestige like the rest of the
shop. A synergy is offered once its target's rune tier is reached and at
least one of the target is owned; the shop shows a row only while it can be
bought, and owned ones collapse into a "Synergies — n of 9 active" line.

| Synergy | Source → target | Cost |
|---|---|---|
| Essence sorting | Rune essence miner → Pure essence miner | 15,000 |
| Essence bait | Pure essence miner → Abyssal leech | 100,000 |
| Abyssal shortcuts | Abyssal leech → ZMI altar trips | 750,000 |
| Ourania offcuts | ZMI altar trips → Wicked hood | 4,000,000 |
| Hooded runners | Wicked hood → Abyss runner | 24,000,000 |
| Blood trail | Abyss runner → Blood altar runner | 150,000,000 |
| Blood for the rift | Blood altar runner → Guardians of the Rift | 900,000,000 |
| Guardian siphons | Guardians of the Rift → Runespan siphon | 5,400,000,000 |
| Practised hands | every click also earns 5% of one second of building income | 250,000 |

Building synergies cost 10× the target's base cost. "Practised hands" needs
any one building; it counts building income before auto-clicks, and crits
and Wizard Finix's auto-clicks include it.

One-offs: pouch ladder Small→Colossal (500 / 5k / 50k / 500k / 5M, each ×2
points/click); Daeyalt shard infusion (25,000, then an activatable ×2-for-60s
boost on a 5-min cooldown).

## Achievements

Plugin-wide, not clicker-only — see `achievements/AchievementManager`. Viewed
in a collapsible section at the bottom of the tab. Clicker achievements cover
lifetime points, clicks, prestiges, tiers, ascension, golden runes (caught and
missed), crits, maxed pouches, big spending, and full attunement.

## Persistence

Per character via `setRSProfileConfiguration`, one pipe-delimited string
(`STATE_KEY`), versioned (currently v5, which added the owned synergies as a
bitmask; older saves load with defaults). Points
are stored as `double`. Autosave every ~10 s and on prestige/purchase.

**No offline or logged-out progress.** The panel checks real game state each
tick: logging out freezes the clicker, logging in loads the character's state
before any income accrues. Saves refuse to write unless the active character
matches the loaded state, and a transient missing-save read keeps in-memory
progress rather than wiping it — these guard against the login-timing wipe bug
found in an early build.

A **Reset progress** button (bottom of the tab) wipes clicker state for the
current character after confirmation; achievements are kept.

## Ideas / not done

- Party leaderboard (broadcast lifetime points over the party websocket)
- Idle-only play is slow in the first few tiers (miners are weak next to
  clicking)
