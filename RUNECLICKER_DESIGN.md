# Runeclicker — design & mechanics

Cookie-clicker, RS-modified: click a rune to craft, spend points on upgrades
that craft for you, and prestige up the rune tiers. Replaces the old cookie
clicker (existing cookie counts migrate into starting points, once, per
character). All numbers live as constants at the top of `RuneClickerPanel` and
are meant to be tuned.

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
prices then produce double, making rebuilds fast.

## Prestige tiers

Real Runecrafting unlock order, 15 tiers: Air, Mind, Water, Earth, Fire, Body,
Cosmic, Chaos, Astral, Nature, Law, Death, Blood, Soul, Wrath.

Cost is the **real OSRS experience** for a mapped level (tiers spread across
levels 20 → 99 via the authentic `xpForLevel` formula), scaled by a per-tier
ramp (`PRESTIGE_TIER_RAMP` = 1.4). The XP curve alone grows ~1.9×/tier — under
income's ×2 — so tiers would get easier; the ramp pushes effective growth to
~2.6×/tier, so each prestige takes progressively longer. First prestige ~4,470;
final Ascend ~870 million.

History: ×10/tier (diverged) → ×3/tier → pure OSRS XP (too fast) → OSRS XP ×
1.4^tier after playtesting. Raise the ramp to steepen, lower it to flatten.

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
- Balance pass on mid/late tiers once more players have climbed
