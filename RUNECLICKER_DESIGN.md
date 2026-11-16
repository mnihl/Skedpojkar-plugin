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
`pointsPerSecond = Σ(idle upgrade rate × count) × globalMultiplier + autoClicks`
`globalMultiplier = 2^tier × 3^attunement` (×2 during Daeyalt, ×7 during frenzy)

So each prestige doubles the whole economy — the same upgrades at the same
prices then produce double, making rebuilds fast. Prestige costs double per
tier as well (see below), so the doubling speeds up the rebuild, not the tier.

## Prestige tiers

Real Runecrafting unlock order, 15 tiers: Air, Mind, Water, Earth, Fire, Body,
Cosmic, Chaos, Astral, Nature, Law, Death, Blood, Soul, Wrath.

`cost = xpForLevel(PRESTIGE_LEVELS[tier]) × 2^tier × ASCENSION_COST_FACTOR^ascensions`

The base is the **real OSRS experience** for a level, via the authentic
`xpForLevel` formula. Each tier has its own level in the `PRESTIGE_LEVELS`
table, ending at 126 (the 200M XP cap). The `× 2^tier` cancels the income
doubling: without it, upgrade prices stay flat while income doubles, and the
late tiers took seconds (a full ascension took under 7 minutes of clicking).

| Prestige | Level | Cost |
|---|---|---|
| Air → Mind | 14 | 2,107 |
| Mind → Water | 29 | 24,062 |
| Water → Earth | 43 | 201,356 |
| Earth → Fire | 55 | 1,333,088 |
| Fire → Body | 67 | 8,767,248 |
| Body → Cosmic | 80 | 63,554,176 |
| Cosmic → Chaos | 90 | 342,165,248 |
| Chaos → Astral | 97 | 1,368,656,512 |
| Astral → Nature | 103 | 4,958,461,952 |
| Nature → Law | 107 | 14,736,419,328 |
| Law → Death | 111 | 43,796,276,224 |
| Death → Blood | 115 | 130,161,547,264 |
| Blood → Soul | 119 | 386,837,450,752 |
| Soul → Wrath | 122 | 1,041,287,249,920 |
| Wrath → Ascend | 126 | 3,094,687,580,160 |

**Pacing.** The levels were fitted with `RuneClickerSimulation` so that, for a
player clicking once a second, each tier takes about 1.3× as long as the one
before: about 4 minutes for Air, about 2.7 hours for Wrath. Simulated time to
the first ascension (golden runes and Daeyalt ignored, so real play is a
little faster):

| Player | First ascension |
|---|---|
| Idle only | ~19 h |
| 1 click/s | ~11 h |
| 3 clicks/s | ~7 h |

To retune, change a level in the table and rerun the simulation; one level is
about 10% more cost.

**After ascension** all prestige costs are multiplied by
`ASCENSION_COST_FACTOR^ascensions` (factor 4.0). It has to exceed attunement's
×3 income per point, otherwise each ascension makes the next run easier; at
4.0 each run is a little longer than the last. Set the factor to 1.0 to
disable.

History: ×10/tier (diverged) → ×3/tier → OSRS XP to level 99 (too fast) → OSRS
XP to level 126 + per-ascension scaling ×2 (v1.0; late tiers took seconds) →
fitted level table × 2^tier, ascension scaling ×4.

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
- Refit `PRESTIGE_LEVELS` after adding buildings or synergies (they speed
  everything up)
- Idle-only play is slow at the start of each tier (miners are weak next to
  clicking); synergies that tie clicks to idle income would narrow the gap
