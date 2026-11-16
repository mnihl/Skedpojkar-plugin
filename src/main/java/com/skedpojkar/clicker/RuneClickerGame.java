package com.skedpojkar.clicker;

import java.util.Arrays;
import java.util.Random;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * The rules and state of Runeclicker, with no UI and no RuneLite in it — so
 * it can be unit-tested and simulated without a game client. The side panel
 * ({@code RuneClickerPanel}) owns one of these and only draws it.
 *
 * Anything time-limited (Daeyalt boost, golden frenzy) takes the current time
 * as a parameter instead of reading the clock, and all randomness comes from
 * the {@link Random} passed in, so tests can control both.
 *
 * All numbers are balance constants meant to be tuned — see
 * RUNECLICKER_DESIGN.md.
 */
@Getter
public class RuneClickerGame
{
	public static final int STATE_VERSION = 4;

	public static final int BULK_BUY_COUNT = 10;

	public static final String[] RUNE_NAMES = {
		"Air", "Mind", "Water", "Earth", "Fire", "Body", "Cosmic", "Chaos",
		"Astral", "Nature", "Law", "Death", "Blood", "Soul", "Wrath",
	};
	public static final int MAX_TIER = RUNE_NAMES.length - 1;
	public static final int NATURE_TIER = 9;

	// Repeatable shop upgrades: cost = base * 1.15^owned. Index 0 adds to
	// points per click; the rest generate points per second. The talisman is
	// named after the current tier's rune ("Mind talisman", ...).
	public static final String[] UPGRADE_NAMES = {
		"talisman", "Rune essence miner", "Pure essence miner",
		"Abyssal leech", "ZMI altar trips", "Wicked hood",
		"Abyss runner", "Blood altar runner", "Guardians of the Rift", "Runespan siphon",
	};
	public static final double[] UPGRADE_BASE_COSTS = {
		50, 200, 1_500, 10_000, 75_000, 400_000,
		2_400_000, 15_000_000, 90_000_000, 540_000_000,
	};
	public static final double[] UPGRADE_IDLE_RATES = {
		0, 0.5, 2, 8, 30, 150,
		750, 4_000, 20_000, 100_000,
	};
	// The rune tier at which each upgrade appears in the shop (0 = from the
	// start). The four big buildings arrive at Fire, Chaos, Law and Soul, so
	// prestiging keeps unlocking something new.
	public static final int[] UPGRADE_UNLOCK_TIERS = {0, 0, 0, 0, 0, 0, 4, 7, 10, 13};
	// Owning this many of an idle building doubles that building's output,
	// once per threshold reached (so x16 at 100 owned). Not for talismans.
	public static final int[] MILESTONE_COUNTS = {10, 25, 50, 100};
	public static final double UPGRADE_COST_GROWTH = 1.15;
	public static final int ZMI_INDEX = 4;
	public static final double ZMI_FAIL_CHANCE = 0.10;

	// One-off pouch ladder: each stage doubles points per click
	public static final String[] POUCH_NAMES = {"Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch"};
	public static final double[] POUCH_COSTS = {500, 5_000, 50_000, 500_000, 5_000_000};

	// Daeyalt shard infusion: one-off purchase, then an activatable x2 boost
	public static final double DAEYALT_COST = 25_000;
	public static final int DAEYALT_BOOST_SECONDS = 60;
	public static final int DAEYALT_COOLDOWN_SECONDS = 300;

	// Clicks have a small chance to crit for x10
	public static final double CRIT_CHANCE = 0.05;
	public static final int CRIT_MULTIPLIER = 10;

	// Golden rune: appears at a random interval, stays clickable briefly.
	// Reward is a points jackpot or a temporary x7 frenzy, 50/50.
	public static final int GOLDEN_MIN_WAIT_S = 180;
	public static final int GOLDEN_EXTRA_WAIT_S = 420;
	public static final int GOLDEN_VISIBLE_S = 10;
	public static final int FRENZY_SECONDS = 30;
	public static final int FRENZY_MULTIPLIER = 7;

	// Runespan perks, bought with points earned by ascending (1 each).
	// AUTO_CLICK and ATTUNEMENT are repeatable; the rest are one-offs.
	public static final int PERK_AUTO_CLICK = 0;
	public static final int PERK_POUCH_KEEPER = 1;
	public static final int PERK_PERFECT_ZMI = 2;
	public static final int PERK_GOLDEN_MAGNET = 3;
	public static final int PERK_ATTUNEMENT = 4;
	public static final String[] PERK_NAMES = {
		"Wizard Finix's help", "Pouch keeper", "Perfect Ourania", "Golden magnetism", "Runespan attunement",
	};
	public static final String[] PERK_DESCRIPTIONS = {
		"Auto-clicks the rune once per second per level (full click value).",
		"Pouches survive prestige.",
		"ZMI altar trips never fail.",
		"Golden runes appear twice as often.",
		"All income x3, permanently. Stacks.",
	};
	public static final boolean[] PERK_REPEATABLE = {true, false, false, false, true};

	// Prestige cost for a tier = the real OSRS experience for that tier's level
	// below, times PRESTIGE_TIER_FACTOR^tier. Income doubles every prestige while
	// shop prices stay flat, and bigger buildings unlock on the way up, so the
	// cost has to grow much faster than income or later tiers take seconds.
	// The levels were fitted with RuneClickerSimulation so each tier takes about
	// 1.3x as long as the one before. They are not strictly rising: a tier that
	// unlocks a new building needs a jump, the tiers after it less. What must
	// rise is the resulting cost (a test checks that). To retune: change a
	// level, rerun the simulation, read the table. One level is ~10% more cost.
	public static final int[] PRESTIGE_LEVELS = {
		14, 22, 30, 36, 43, 57, 67, 88, 90, 95, 109, 106, 103, 116, 112,
	};
	public static final double PRESTIGE_TIER_FACTOR = 4.0;
	// Each completed ascension multiplies all prestige costs. It must be well
	// above attunement's x3 income per level, or every ascension makes the next
	// run easier than the last. 1.0 disables.
	public static final double ASCENSION_COST_FACTOR = 5.0;

	/** What one click of the rune produced. */
	public static final class Click
	{
		public final double gained;
		public final boolean crit;

		Click(double gained, boolean crit)
		{
			this.gained = gained;
			this.crit = crit;
		}
	}

	@Getter(AccessLevel.NONE)
	private final Random random;

	// Persisted per character (see serialize/load)
	private int tier;
	private double points;
	private int ascensions;
	@Getter(AccessLevel.NONE)
	private final int[] upgradeCounts = new int[UPGRADE_NAMES.length];
	private int pouchLevel;
	private boolean daeyaltOwned;
	private double lifetimePoints;
	private long totalClicks;
	private int totalPrestiges;
	private long milestoneMask;
	private long goldenSeen;
	private long goldenCaught;
	private long critCount;
	@Getter(AccessLevel.NONE)
	private final int[] perkCounts = new int[PERK_NAMES.length];

	// Not persisted: temporary boosts end when the client closes
	private long daeyaltBoostEndMs;
	private long daeyaltCooldownEndMs;
	private long frenzyEndMs;

	public RuneClickerGame(Random random)
	{
		this.random = random;
	}

	public int getUpgradeCount(int idx)
	{
		return upgradeCounts[idx];
	}

	public int getPerkCount(int idx)
	{
		return perkCounts[idx];
	}

	// ---- Income ----

	/**
	 * Applies to ALL income, clicks and idle alike: x2 per rune tier, x3 per
	 * Runespan attunement perk, x2 during Daeyalt, x7 during a golden frenzy.
	 */
	public double globalMultiplier(long nowMs)
	{
		double mult = Math.pow(2, tier) * Math.pow(3, perkCounts[PERK_ATTUNEMENT]);
		if (isDaeyaltActive(nowMs))
		{
			mult *= 2;
		}
		if (isFrenzyActive(nowMs))
		{
			mult *= FRENZY_MULTIPLIER;
		}
		return mult;
	}

	public double pointsPerClick(long nowMs)
	{
		double base = 1 + upgradeCounts[0];
		return base * Math.pow(2, pouchLevel) * globalMultiplier(nowMs);
	}

	public double zmiYieldFactor()
	{
		return perkCounts[PERK_PERFECT_ZMI] > 0 ? 1 : 1 - ZMI_FAIL_CHANCE;
	}

	/** x2 for every ownership milestone (10, 25, 50, 100) reached with this many owned. */
	public static int milestoneMultiplier(int owned)
	{
		int mult = 1;
		for (int threshold : MILESTONE_COUNTS)
		{
			if (owned >= threshold)
			{
				mult *= 2;
			}
		}
		return mult;
	}

	/** The next ownership milestone above this many owned, or -1 once all are reached. */
	public static int nextMilestone(int owned)
	{
		for (int threshold : MILESTONE_COUNTS)
		{
			if (owned < threshold)
			{
				return threshold;
			}
		}
		return -1;
	}

	/**
	 * What {@code owned} of an idle building produce per second together,
	 * milestone doublings included, before the global multiplier.
	 */
	public static double buildingOutput(int idx, int owned)
	{
		return UPGRADE_IDLE_RATES[idx] * owned * milestoneMultiplier(owned);
	}

	/** Expected idle income per second (ZMI at its average yield; auto-clicks included). */
	public double pointsPerSecondExpected(long nowMs)
	{
		double sum = 0;
		for (int i = 1; i < UPGRADE_NAMES.length; i++)
		{
			double rate = buildingOutput(i, upgradeCounts[i]);
			if (i == ZMI_INDEX)
			{
				rate *= zmiYieldFactor();
			}
			sum += rate;
		}
		return sum * globalMultiplier(nowMs) + perkCounts[PERK_AUTO_CLICK] * pointsPerClick(nowMs);
	}

	/** Adds points to both the spendable balance and the lifetime total. */
	public void gainPoints(double amount)
	{
		points += amount;
		lifetimePoints += amount;
	}

	/** One click of the rune: a small chance to crit for x10. */
	public Click click(long nowMs)
	{
		totalClicks++;
		double gained = pointsPerClick(nowMs);
		boolean crit = random.nextDouble() < CRIT_CHANCE;
		if (crit)
		{
			gained *= CRIT_MULTIPLIER;
			critCount++;
		}
		gainPoints(gained);
		return new Click(gained, crit);
	}

	/**
	 * One second of idle income. ZMI rolls its 10% failure here, so this is
	 * the real (random) income, not the expected figure. Returns what was earned.
	 */
	public double tick(long nowMs)
	{
		double income = 0;
		for (int i = 1; i < UPGRADE_NAMES.length; i++)
		{
			double rate = buildingOutput(i, upgradeCounts[i]);
			if (i == ZMI_INDEX && perkCounts[PERK_PERFECT_ZMI] == 0
				&& random.nextDouble() < ZMI_FAIL_CHANCE)
			{
				rate = 0; // it's random, it's ZMI
			}
			income += rate;
		}
		income *= globalMultiplier(nowMs);
		// Wizard Finix clicks for you (full click value, crits not included)
		income += perkCounts[PERK_AUTO_CLICK] * pointsPerClick(nowMs);
		gainPoints(income);
		return income;
	}

	// ---- Shop ----

	public double upgradeCost(int idx)
	{
		return UPGRADE_BASE_COSTS[idx] * Math.pow(UPGRADE_COST_GROWTH, upgradeCounts[idx]);
	}

	/** False while the current rune tier is too low for this upgrade to be sold. */
	public boolean isUpgradeUnlocked(int idx)
	{
		return tier >= UPGRADE_UNLOCK_TIERS[idx];
	}

	/** The first upgrade the current tier doesn't sell yet, or -1 if everything is unlocked. */
	public int nextLockedUpgrade()
	{
		for (int i = 0; i < UPGRADE_NAMES.length; i++)
		{
			if (!isUpgradeUnlocked(i))
			{
				return i;
			}
		}
		return -1;
	}

	/** Buys one (or up to {@link #BULK_BUY_COUNT}) of an upgrade. Returns how many were bought. */
	public int buyUpgrade(int idx, boolean bulk)
	{
		if (!isUpgradeUnlocked(idx))
		{
			return 0;
		}
		int toBuy = bulk ? BULK_BUY_COUNT : 1;
		int bought = 0;
		for (int i = 0; i < toBuy; i++)
		{
			double cost = upgradeCost(idx);
			if (points < cost)
			{
				break;
			}
			points -= cost;
			upgradeCounts[idx]++;
			bought++;
		}
		return bought;
	}

	public boolean hasAllPouches()
	{
		return pouchLevel >= POUCH_NAMES.length;
	}

	/** Cost of the next pouch. Only valid while {@link #hasAllPouches()} is false. */
	public double nextPouchCost()
	{
		return POUCH_COSTS[pouchLevel];
	}

	public boolean buyPouch()
	{
		if (hasAllPouches() || points < nextPouchCost())
		{
			return false;
		}
		points -= nextPouchCost();
		pouchLevel++;
		return true;
	}

	public boolean buyDaeyalt()
	{
		if (daeyaltOwned || points < DAEYALT_COST)
		{
			return false;
		}
		points -= DAEYALT_COST;
		daeyaltOwned = true;
		return true;
	}

	/** Starts the Daeyalt boost if it is owned and off cooldown. */
	public boolean activateDaeyalt(long nowMs)
	{
		if (!daeyaltOwned || nowMs < daeyaltCooldownEndMs)
		{
			return false;
		}
		daeyaltBoostEndMs = nowMs + DAEYALT_BOOST_SECONDS * 1000L;
		daeyaltCooldownEndMs = nowMs + DAEYALT_COOLDOWN_SECONDS * 1000L;
		return true;
	}

	public boolean isDaeyaltActive(long nowMs)
	{
		return nowMs < daeyaltBoostEndMs;
	}

	public boolean isFrenzyActive(long nowMs)
	{
		return nowMs < frenzyEndMs;
	}

	// ---- Golden runes ----

	/** Seconds until the next golden rune should appear (a fresh random roll each call). */
	public int nextGoldenWaitSeconds()
	{
		int wait = GOLDEN_MIN_WAIT_S + random.nextInt(GOLDEN_EXTRA_WAIT_S);
		if (perkCounts[PERK_GOLDEN_MAGNET] > 0)
		{
			wait /= 2;
		}
		return wait;
	}

	public void goldenAppeared()
	{
		goldenSeen++;
	}

	/**
	 * Catches a golden rune: 50/50 a points jackpot or a x7 frenzy.
	 * Returns the jackpot gained, or 0 if a frenzy started instead.
	 */
	public double catchGolden(long nowMs)
	{
		goldenCaught++;
		if (random.nextBoolean())
		{
			// Jackpot: a solid chunk relative to your economy
			double jackpot = Math.max(pointsPerClick(nowMs) * 50, pointsPerSecondExpected(nowMs) * 300);
			jackpot = Math.max(jackpot, 500); // floor for early game
			gainPoints(jackpot);
			return jackpot;
		}
		frenzyEndMs = nowMs + FRENZY_SECONDS * 1000L;
		return 0;
	}

	// ---- Prestige and ascension ----

	/**
	 * Prestige cost is the real OSRS experience for the tier's level (see
	 * {@link #PRESTIGE_LEVELS}), times 4^tier, times a per-ascension factor.
	 * First prestige 2,107 (level 14); final Ascend ~12.7 quadrillion, before
	 * ascension scaling.
	 */
	public double prestigeCost()
	{
		return Math.floor(xpForLevel(PRESTIGE_LEVELS[tier]) * Math.pow(PRESTIGE_TIER_FACTOR, tier)
			* Math.pow(ASCENSION_COST_FACTOR, ascensions));
	}

	/** Total OSRS experience required to reach the given level. */
	public static double xpForLevel(int level)
	{
		double points = 0;
		for (int lvl = 1; lvl < level; lvl++)
		{
			points += Math.floor(lvl + 300 * Math.pow(2, lvl / 7.0));
		}
		return Math.floor(points / 4);
	}

	/** True at Wrath, where the next prestige is an ascension instead. */
	public boolean isAscensionNext()
	{
		return tier >= MAX_TIER;
	}

	public boolean canPrestige()
	{
		return points >= prestigeCost();
	}

	/**
	 * Resets points and upgrades and moves up one rune tier — or, at Wrath,
	 * ascends: back to Air with one more Runespan point. Returns false (and
	 * changes nothing) if the cost isn't met.
	 */
	public boolean prestige()
	{
		if (!canPrestige())
		{
			return false;
		}
		boolean ascend = isAscensionNext();
		boolean keepPouches = perkCounts[PERK_POUCH_KEEPER] > 0;

		points = 0;
		Arrays.fill(upgradeCounts, 0);
		if (!keepPouches || ascend)
		{
			pouchLevel = 0;
		}
		daeyaltOwned = false;
		daeyaltBoostEndMs = 0;
		daeyaltCooldownEndMs = 0;
		frenzyEndMs = 0;
		totalPrestiges++;
		if (ascend)
		{
			tier = 0;
			ascensions++;
		}
		else
		{
			tier++;
		}
		return true;
	}

	public int unspentRunespanPoints()
	{
		int spent = 0;
		for (int c : perkCounts)
		{
			spent += c;
		}
		return ascensions - spent;
	}

	public boolean isPerkMaxed(int idx)
	{
		return !PERK_REPEATABLE[idx] && perkCounts[idx] > 0;
	}

	public boolean buyPerk(int idx)
	{
		if (unspentRunespanPoints() < 1 || isPerkMaxed(idx))
		{
			return false;
		}
		perkCounts[idx]++;
		return true;
	}

	// ---- Persistence ----

	/** True once this state holds anything worth protecting from an accidental wipe. */
	public boolean hasProgress()
	{
		return lifetimePoints > 0 || tier > 0 || totalClicks > 0;
	}

	/** Back to a brand-new game. */
	public void reset()
	{
		tier = 0;
		points = 0;
		ascensions = 0;
		pouchLevel = 0;
		daeyaltOwned = false;
		lifetimePoints = 0;
		totalClicks = 0;
		totalPrestiges = 0;
		milestoneMask = 0;
		goldenSeen = 0;
		goldenCaught = 0;
		critCount = 0;
		daeyaltBoostEndMs = 0;
		daeyaltCooldownEndMs = 0;
		frenzyEndMs = 0;
		Arrays.fill(upgradeCounts, 0);
		Arrays.fill(perkCounts, 0);
	}

	/** A new game that starts with some points (the old cookie-clicker migration). */
	public void resetWithStartingPoints(double startingPoints)
	{
		reset();
		points = startingPoints;
		lifetimePoints = startingPoints;
	}

	/** The whole persisted state as one pipe-delimited string, version first. */
	public String serialize()
	{
		return STATE_VERSION + "|" + tier + "|" + points + "|" + ascensions
			+ "|" + pouchLevel + "|" + (daeyaltOwned ? "1" : "0") + "|" + joinCounts(upgradeCounts)
			+ "|" + lifetimePoints + "|" + totalClicks + "|" + totalPrestiges
			+ "|" + milestoneMask + "|" + joinCounts(perkCounts)
			+ "|" + goldenSeen + "|" + goldenCaught + "|" + critCount;
	}

	/**
	 * Loads a string produced by {@link #serialize()}, including ones written
	 * by older versions (fields they lack get defaults). If the string is
	 * corrupt the game is reset to new and false is returned.
	 */
	public boolean load(String stored)
	{
		try
		{
			String[] parts = stored.split("\\|");
			int version = Integer.parseInt(parts[0]);
			tier = Integer.parseInt(parts[1]);
			points = Double.parseDouble(parts[2]);
			ascensions = Integer.parseInt(parts[3]);
			pouchLevel = Integer.parseInt(parts[4]);
			daeyaltOwned = "1".equals(parts[5]);
			parseCounts(parts[6], upgradeCounts);

			if (version >= 2)
			{
				lifetimePoints = Double.parseDouble(parts[7]);
				totalClicks = Long.parseLong(parts[8]);
				totalPrestiges = Integer.parseInt(parts[9]);
				milestoneMask = Long.parseLong(parts[10]);
				parseCounts(parts[11], perkCounts);
			}
			else
			{
				// v1 predates lifetime/milestone/perk tracking
				lifetimePoints = points;
				totalClicks = 0;
				totalPrestiges = tier + ascensions;
				milestoneMask = 0;
				Arrays.fill(perkCounts, 0);
			}
			goldenSeen = version >= 3 ? Long.parseLong(parts[12]) : 0;
			goldenCaught = version >= 3 ? Long.parseLong(parts[13]) : 0;
			critCount = version >= 4 ? Long.parseLong(parts[14]) : 0;
			return true;
		}
		catch (RuntimeException e)
		{
			reset();
			return false;
		}
	}

	private static void parseCounts(String csv, int[] target)
	{
		String[] counts = csv.split(",");
		for (int i = 0; i < target.length; i++)
		{
			target[i] = i < counts.length ? Integer.parseInt(counts[i]) : 0;
		}
	}

	private static String joinCounts(int[] counts)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < counts.length; i++)
		{
			if (i > 0)
			{
				sb.append(',');
			}
			sb.append(counts[i]);
		}
		return sb.toString();
	}
}
