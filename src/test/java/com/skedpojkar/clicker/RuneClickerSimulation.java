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

			// With no clicking at all nothing can ever be bought, so even the
			// "idle" player clicks until the first miner is running
			int clicks = clicksPerSecond == 0 && game.pointsPerSecondExpected(now) == 0 ? 1 : clicksPerSecond;
			for (int i = 0; i < clicks; i++)
			{
				game.click(now);
			}
			game.tick(now);
			shop(game, now, clicks);
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

	/**
	 * Buys, repeatedly, the affordable purchase that pays for itself fastest —
	 * but only purchases that bring the next prestige closer rather than
	 * delaying it.
	 */
	private static void shop(RuneClickerGame game, long now, int clicksPerSecond)
	{
		while (true)
		{
			double gm = game.globalMultiplier(now);
			double clickRate = clicksPerSecond * AVERAGE_CLICK_FACTOR;
			double income = game.pointsPerSecondExpected(now) + clickRate * game.pointsPerClick(now);
			double missing = game.prestigeCost() - game.getPoints();
			if (missing <= 0)
			{
				return;
			}
			// A purchase is worth it if cost / extra income is under this
			double worthItBelow = income > 0 ? missing / income : Double.MAX_VALUE;

			int best = -1; // upgrade index, or UPGRADE_NAMES.length for the next pouch
			double bestPayback = worthItBelow;
			for (int i = 0; i < UPGRADE_NAMES.length; i++)
			{
				double gain = i == 0
					? clickRate * Math.pow(2, game.getPouchLevel()) * gm
					: UPGRADE_IDLE_RATES[i] * gm * (i == ZMI_INDEX ? game.zmiYieldFactor() : 1);
				double cost = game.upgradeCost(i);
				if (gain > 0 && cost <= game.getPoints() && cost / gain < bestPayback)
				{
					best = i;
					bestPayback = cost / gain;
				}
			}
			if (!game.hasAllPouches())
			{
				double gain = clickRate * game.pointsPerClick(now); // a pouch doubles clicks
				double cost = game.nextPouchCost();
				if (gain > 0 && cost <= game.getPoints() && cost / gain < bestPayback)
				{
					best = UPGRADE_NAMES.length;
				}
			}

			if (best < 0)
			{
				return;
			}
			if (best == UPGRADE_NAMES.length)
			{
				game.buyPouch();
			}
			else
			{
				game.buyUpgrade(best, false);
			}
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
