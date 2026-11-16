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
| Mind → Water | 22 | 22,496 | |
| Water → Earth | 30 | 213,808 | |
| Earth → Fire | 36 | 1,588,160 | Abyss runner |
| Fire → Body | 43 | 12,886,784 | |
| Body → Cosmic | 57 | 208,132,096 | |
| Cosmic → Chaos | 67 | 2,244,415,488 | Blood altar runner |
| Chaos → Astral | 88 | 71,856,553,984 | |
| Astral → Nature | 90 | 350,377,213,952 | |
| Nature → Law | 95 | 2,299,411,300,352 | Guardians of the Rift |
| Law → Death | 109 | 36,789,974,728,704 | |
| Death → Blood | 106 | 109,339,767,472,128 | |
| Blood → Soul | 103 | 324,957,762,486,272 | Runespan siphon |
| Soul → Wrath | 116 | ~4.71 quadrillion | |
| Wrath → Ascend | 112 | ~12.68 quadrillion | |

The levels are not strictly rising: a tier where a new building has just
unlocked needs a big jump in cost, the tiers after it less. The cost itself
always rises.

**Pacing.** The levels were fitted with `RuneClickerSimulation` so that, for a
player clicking once a second, each tier takes about 1.3× as long as the one
before: about 4 minutes for Air, about 2.7 hours for Wrath. Simulated time to
the first ascension (golden runes and Daeyalt ignored, so real play is a
little faster):

| Player | First ascension |
|---|---|
| Idle only | ~13 h |
| 1 click/s | ~11.5 h |
| 3 clicks/s | ~11 h |

Clicking matters most in the early tiers; from about Cosmic on, buildings and
their milestone doublings carry the income and idle play keeps up.

To retune, change a level in the table and rerun the simulation; one level is
about 10% more cost. Refit after any change to buildings or multipliers.

**After ascension** all prestige costs are multiplied by
`ASCENSION_COST_FACTOR^ascensions` (factor 5.0). It has to be well above
attunement's ×3 income per point, otherwise each ascension makes the next run
easier; at 5.0 the second run takes about 14 hours and the third about 15.
Set the factor to 1.0 to disable.

History: ×10/tier (diverged) → ×3/tier → OSRS XP to level 99 (too fast) → OSRS
XP to level 126 + per-ascension scaling ×2 (v1.0; late tiers took seconds) →
fitted level table × 2^tier, ascension ×4 → four more buildings, tier unlocks
and ownership milestones; refitted table × 4^tier, ascension ×5.

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

Repeatable cost = `baseCost × 1.15^owned`; reset on prestige.

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
(`STATE_KEY`), versioned (currently v4; older saves load with defaults). Points
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
- Buy-max (currently shift-click buys up to 10)
- Cross-building synergies (one building boosting another); refit
  `PRESTIGE_LEVELS` afterwards
- Idle-only play is slow in the first few tiers (miners are weak next to
  clicking); late tiers are the reverse, clicking barely matters there
