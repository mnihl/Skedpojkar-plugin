# Runecrafting Clicker — design proposal

Cookie clicker, RS-modified: click to craft runes, spend points in a shop on
things that craft for you, prestige to the next rune tier for a permanent
click multiplier. Replaces (or sits beside) the current Cookies tab.

## Core loop

- The big button is the current rune (its actual game icon). Each click crafts
  runes worth your **points per click**.
- Points buy **shop upgrades**: some add to points per click, some generate
  points per second (idle), even while the panel is closed (while logged in).
- When you can afford the **prestige cost**, you may reset points and all
  upgrades to zero; in exchange the button becomes the next rune and your base
  points per click doubles, permanently.

## Prestige tiers

Follow the real Runecrafting unlock order, 15 tiers:

Air → Mind → Water → Earth → Fire → Body → Cosmic → Chaos → Astral → Nature →
Law → Death → Blood → Soul → Wrath

| Tier | Rune | Global income multiplier | Prestige cost (to leave this tier) |
|---|---|---|---|
| 0 | Air | ×1 | 10,000 |
| 1 | Mind | ×2 | 100,000 |
| 2 | Water | ×4 | 1,000,000 |
| ... | ... | ×2^tier | 10,000 × 10^tier |
| 14 | Wrath | ×16,384 | — (max; postgame = big numbers) |

The tier multiplier applies to ALL income — clicks and idle — so after a
prestige the same upgrades at the same prices produce double, and rebuilding
takes roughly half the time. (Originally it only boosted clicks, which made
prestige feel like starting over; changed after playtesting.)

(The friend's example jumped Air → Earth; using the real unlock order gives us
15 tiers for free and reads as authentically RS. Costs are placeholder — tune
so each tier takes noticeably longer than the last but idle income keeps it
moving.)

## Shop (RS-flavored upgrades)

Each upgrade is buyable repeatedly; cost scales cookie-clicker style:
`cost = baseCost × 1.15^owned`. Upgrades reset on prestige.

| Upgrade | Effect | Base cost | Flavor |
|---|---|---|---|
| Chisel-sharpened talisman | +1 point/click | 50 | click upgrade |
| Small → Colossal pouch (5 stages, one-off each) | ×2 points/click each | 500 / 5k / 50k / 500k / 5M | the real pouch ladder |
| Rune essence miner | +0.5 points/s | 200 | idle |
| Pure essence miner | +2 points/s | 1,500 | idle |
| Abyssal leech | +8 points/s | 10,000 | idle |
| ZMI altar trips | +30 points/s, but 10% of ticks give 0 (it's random, it's ZMI) | 75,000 | idle, jokey |
| Wicked hood | +150 points/s | 400,000 | idle |
| Daeyalt shard infusion | ×2 all income for 60 s, 10 min cooldown (active button) | 25,000 | the "golden cookie" |

Roughly: clicks dominate early, idle takes over mid-tier, prestige multiplier
is what actually moves you forward. Exact numbers need play-tuning.

## UI (fits the narrow sidebar)

- Top: rune icon button (click target), points count, points/s readout.
- Middle: scrollable shop list — name, owned count, cost, buy button;
  unaffordable entries greyed out.
- Bottom: prestige button showing next rune + cost, with a confirm click
  ("Reset everything for Mind runes? This cannot be undone.").
- Rune icons: RuneLite's `ItemManager.getImage(itemId)` gives real item
  sprites — no bundled art needed.

## Persistence & implementation notes

- Per character via `setRSProfileConfiguration` (same pattern as the current
  cookie count). Store one serialized string: `tier|points|upgrade counts...`.
  Points as `long` (doubling caps at tier 14 → no overflow risk); display
  formatted (12.3K / 4.5M / 1.2B).
- Idle tick: one Swing `Timer` at 1 s in the panel adds points/s and refreshes
  labels. Runs while the client is open regardless of which tab is showing;
  save to config at most every ~10 s and on shutdown, not every tick.
- No offline progress (config would allow it via a timestamp, but "your miner
  worked while you were gone" invites save-scumming complaints; decide later).
- All purely client-side UI — zero game interaction, no Hub-eligibility
  concerns.

## Decisions (settled)

- **Replaces the Cookies tab.** Existing cookie counts migrate into starting
  points, one-time, per character (the count is read once from the old config
  key and credited as points).
- **No offline progress.** Idle income accrues only while the client is open
  and you're logged in; nothing is earned between sessions.
- **Second prestige layer: Runespan Ascension (implemented as a perk shop).**
  Ascending resets everything and grants 1 Runespan point. Points buy
  permanent perks (1 point each): Wizard Finix's help (auto-click 1/s per
  level, repeatable), Pouch keeper (pouches survive prestige), Perfect
  Ourania (ZMI never fails), Golden magnetism (golden runes 2x as often),
  Runespan attunement (all income x3, repeatable). This replaced the original
  flat x10-per-ascension idea — decisions beat numbers.

## Also implemented (v2)

- **Golden rune**: appears every 3–10 min for 10 s; clicking gives a points
  jackpot or a x7 income frenzy for 30 s (50/50).
- **Crit clicks**: 5% chance of x10 per manual click, with orange feedback.
- **Milestones**: one-time chat messages (no sounds) for lifetime points
  (10K/1M/1B/1T), 1,000 manual clicks, first prestige, reaching Nature,
  reaching Wrath, first ascension. Tracked in a persisted bitmask.
- **Juice**: click feedback label under the rune ("+208" / "CRIT! +2,080"),
  stats footer (lifetime points, clicks, prestiges, ascensions), talisman
  named after the current tier ("Mind talisman").
- Save format bumped to v2 (v1 saves load with sensible defaults).
