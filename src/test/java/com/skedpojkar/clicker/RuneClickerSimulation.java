package com.skedpojkar.clicker;

import static com.skedpojkar.clicker.RuneClickerGame.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

/**
 * Plays Runeclicker automatically, far faster than real time, to measure how
 * long each prestige tier takes. Use it when tuning balance numbers: change
 * a constant in {@link RuneClickerGame}, run this, read the table.
 *
 * Run the {@code main} method (Run button above it in VS Code) to print the
 * table. The same simulation also runs as a test during `./gradlew build`, as
 * a check that the game can still be completed.
 *
 * The simulated player clicks a fixed number of times per second, buys
 * whatever pays for itself fastest (if it would bring the next prestige
 * closer), and prestiges the moment it can. It ignores golden runes and the
 * Daeyalt boost, so a real attentive player is somewhat faster than this.
 */
public class RuneClickerSimulation
{
	private static final double AVERAGE_CLICK_FACTOR = 1 + CRIT_CHANCE * (CRIT_MULTIPLIER - 1);
	private static final long GIVE_UP_AFTER_SECONDS = 365L * 24 * 3600;

	public static void main(String[] args)
	{
		if (args.length > 0 && args[0].equals("fit"))
		{
			// fit <hours for the Wrath tier> <growth per tier> <clicks per second>
			System.out.println(fitLevels(Double.parseDouble(args[1]) * 3600,
				Double.parseDouble(args[2]), Integer.parseInt(args[3])));
			return;
		}
		for (int clicksPerSecond : new int[]{0, 1, 3, 6})
		{
			System.out.println(report(clicksPerSecond, 2));
		}
	}

	@Test
	public void theGameCanBeCompletedAndReportsItsPacing()
	{
		System.out.println(report(0, 2));
		System.out.println(report(3, 2));

		for (long[] tiers : simulate(3, 2, 1))
		{
			assertEquals(MAX_TIER + 1, tiers.length);
			for (long seconds : tiers)
			{
				assertTrue(seconds > 0);
			}
		}
	}

	/** A printable table of time per tier, for each of the first {@code ascensions} runs. */
	public static String report(int clicksPerSecond, int ascensions)
	{
		long[][] runs = simulate(clicksPerSecond, ascensions, 1);
		StringBuilder sb = new StringBuilder();
		sb.append("Runeclicker pacing at ").append(clicksPerSecond).append(" clicks/s\n");
		for (int run = 0; run < runs.length; run++)
		{
			long total = 0;
			sb.append("  Run ").append(run + 1).append(run == 0 ? "" : " (attunement x" + run + ")").append('\n');
			for (int tier = 0; tier <= MAX_TIER; tier++)
			{
				total += runs[run][tier];
				sb.append(String.format("    %-7s %10s%n", RUNE_NAMES[tier], duration(runs[run][tier])));
			}
			sb.append(String.format("    %-7s %10s%n", "TOTAL", duration(total)));
		}
		return sb.toString();
	}

	/**
	 * Suggests a {@link RuneClickerGame#PRESTIGE_LEVELS} table: for each tier,
	 * the level whose cost takes the simulated player closest to a target time.
	 * The Wrath tier's target is {@code wrathSeconds}; each tier below it gets
	 * that divided by {@code growth} once more. Run it after changing anything
	 * that affects income, then paste the levels into RuneClickerGame.
	 */
	public static String fitLevels(double wrathSeconds, double growth, int clicksPerSecond)
	{
		StringBuilder sb = new StringBuilder("Fitted PRESTIGE_LEVELS: ");
		for (int tier = 0; tier <= MAX_TIER; tier++)
		{
			double target = wrathSeconds / Math.pow(growth, MAX_TIER - tier);
			double factor = Math.pow(PRESTIGE_TIER_FACTOR, tier);
			int low = 2;
			int high = 160;
			while (low < high)
			{
				int mid = (low + high) / 2;
				if (tierSeconds(tier, xpForLevel(mid) * factor, clicksPerSecond) < target)
				{
					low = mid + 1;
				}
				else
				{
					high = mid;
				}
			}
			// low is the first level that takes at least the target; the one below may be closer
			long above = tierSeconds(tier, xpForLevel(low) * factor, clicksPerSecond);
			long below = tierSeconds(tier, xpForLevel(low - 1) * factor, clicksPerSecond);
			int level = low > 2 && target - below < above - target ? low - 1 : low;
			sb.append(level).append(tier < MAX_TIER ? ", " : "");
		}
		return sb.toString();
	}

	/** Seconds a first-run player needs at this tier to gather {@code cost} points. */
	static long tierSeconds(int tier, double cost, int clicksPerSecond)
	{
		RuneClickerGame game = new RuneClickerGame(new Random(1));
		game.load(STATE_VERSION + "|" + tier + "|0.0|0|0|0|0|0.0|0|0|0|0|0|0|0|0");
		long elapsed = 0;
		while (game.getPoints() < cost && elapsed < GIVE_UP_AFTER_SECONDS)
		{
			playOneSecond(game, elapsed * 1000L, clicksPerSecond, cost);
			elapsed++;
		}
		return elapsed;
	}

	/** Clicks, collects a second of idle income and shops towards {@code goal} points. */
	private static void playOneSecond(RuneClickerGame game, long now, int clicksPerSecond, double goal)
	{
		// With no clicking at all nothing can ever be bought, so even the
		// "idle" player clicks until the first miner is running
		int clicks = clicksPerSecond == 0 && game.pointsPerSecondExpected(now) == 0 ? 1 : clicksPerSecond;
		for (int i = 0; i < clicks; i++)
		{
			game.click(now);
		}
		game.tick(now);
		shop(game, now, clicks, goal);
	}

	/**
	 * Plays through {@code ascensions} full runs. Returns, per run, the seconds
	 * spent on each tier. Every Runespan point is spent on attunement.
	 */
	public static long[][] simulate(int clicksPerSecond, int ascensions, long seed)
	{
		RuneClickerGame game = new RuneClickerGame(new Random(seed));
		long[][] seconds = new long[ascensions][MAX_TIER + 1];
		long elapsed = 0;

		while (game.getAscensions() < ascensions)
		{
			long now = elapsed * 1000L;
			int run = game.getAscensions();
			int tier = game.getTier();

			playOneSecond(game, now, clicksPerSecond, game.prestigeCost());
			if (game.prestige())
			{
				while (game.buyPerk(PERK_ATTUNEMENT))
				{
					// spend every Runespan point on the income perk
				}
			}

			seconds[run][tier]++;
			elapsed++;
			if (elapsed > GIVE_UP_AFTER_SECONDS)
			{
				throw new IllegalStateException("Still not done after a simulated year — stuck at run "
					+ (run + 1) + ", " + RUNE_NAMES[tier]);
			}
		}
		return seconds;
	}

	// Purchase numbering for the shopper: upgrades first, then the next pouch,
	// then the synergies
	private static final int POUCH_CHOICE = UPGRADE_NAMES.length;
	private static final int FIRST_SYNERGY_CHOICE = POUCH_CHOICE + 1;
	private static final int CHOICE_COUNT = FIRST_SYNERGY_CHOICE + SYNERGY_NAMES.length;

	/**
	 * Buys, repeatedly, the affordable purchase that pays for itself fastest —
	 * but only purchases that bring {@code goal} points closer rather than
	 * delaying it. What a purchase is worth is measured by trying it on a copy
	 * of the game, so milestones and synergies are counted without this class
	 * repeating their rules.
	 */
	private static void shop(RuneClickerGame game, long now, int clicksPerSecond, double goal)
	{
		double clickRate = clicksPerSecond * AVERAGE_CLICK_FACTOR;
		while (true)
		{
			double income = income(game, now, clickRate);
			double missing = goal - game.getPoints();
			if (missing <= 0)
			{
				return;
			}
			// A purchase is worth it if cost / extra income is under this
			double bestPayback = income > 0 ? missing / income : Double.MAX_VALUE;

			int best = -1;
			for (int choice = 0; choice < CHOICE_COUNT; choice++)
			{
				double cost = costOf(game, choice);
				if (cost > game.getPoints())
				{
					continue; // unaffordable, locked or already owned
				}
				RuneClickerGame trial = game.copy();
				buy(trial, choice);
				double gain = income(trial, now, clickRate) - income;
				if (gain > 0 && cost / gain < bestPayback)
				{
					best = choice;
					bestPayback = cost / gain;
				}
			}

			if (best < 0)
			{
				return;
			}
			buy(game, best);
		}
	}

	private static double income(RuneClickerGame game, long now, double clickRate)
	{
		return game.pointsPerSecondExpected(now) + clickRate * game.pointsPerClick(now);
	}

	/** The price of a purchase, or infinity if it isn't for sale right now. */
	private static double costOf(RuneClickerGame game, int choice)
	{
		if (choice < POUCH_CHOICE)
		{
			return game.isUpgradeUnlocked(choice) ? game.upgradeCost(choice) : Double.POSITIVE_INFINITY;
		}
		if (choice == POUCH_CHOICE)
		{
			return game.hasAllPouches() ? Double.POSITIVE_INFINITY : game.nextPouchCost();
		}
		int synergy = choice - FIRST_SYNERGY_CHOICE;
		return game.isSynergyOwned(synergy) || !game.isSynergyOffered(synergy)
			? Double.POSITIVE_INFINITY : SYNERGY_COSTS[synergy];
	}

	private static void buy(RuneClickerGame game, int choice)
	{
		if (choice < POUCH_CHOICE)
		{
			game.buyUpgrade(choice, false);
		}
		else if (choice == POUCH_CHOICE)
		{
			game.buyPouch();
		}
		else
		{
			game.buySynergy(choice - FIRST_SYNERGY_CHOICE);
		}
	}

	private static String duration(long seconds)
	{
		if (seconds < 3600)
		{
			return String.format("%dm %02ds", seconds / 60, seconds % 60);
		}
		return String.format("%dh %02dm", seconds / 3600, seconds % 3600 / 60);
	}
}
