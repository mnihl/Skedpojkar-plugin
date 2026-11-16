package com.skedpojkar.clicker;

import static com.skedpojkar.clicker.RuneClickerGame.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Runeclicker's rules. Each method marked @Test is one check;
 * `./gradlew build` runs them all and fails the build if any is wrong.
 */
public class RuneClickerGameTest
{
	private static final double EXACT = 1e-9;
	private static final long NOW = 1_000_000L;

	/** A "random" source that returns whatever the test tells it to. */
	private static class FixedRandom extends Random
	{
		double nextDouble = 0.99; // high = no crit, no ZMI failure
		boolean nextBoolean = true;
		int nextInt = 0;

		@Override
		public double nextDouble()
		{
			return nextDouble;
		}

		@Override
		public boolean nextBoolean()
		{
			return nextBoolean;
		}

		@Override
		public int nextInt(int bound)
		{
			return nextInt;
		}
	}

	private FixedRandom random;
	private RuneClickerGame game;

	@Before
	public void newGame()
	{
		random = new FixedRandom();
		game = new RuneClickerGame(random);
	}

	/** Gives the game enough points to prestige, then prestiges. */
	private void prestigeOnce()
	{
		game.gainPoints(game.prestigeCost());
		assertTrue(game.prestige());
	}

	private void ascendOnce()
	{
		for (int i = 0; i <= MAX_TIER; i++)
		{
			prestigeOnce();
		}
	}

	// ---- Costs ----

	@Test
	public void xpForLevelMatchesTheRealOsrsTable()
	{
		assertEquals(0, xpForLevel(1), EXACT);
		assertEquals(83, xpForLevel(2), EXACT);
		assertEquals(13_034_431, xpForLevel(99), EXACT);
		assertEquals(188_884_740, xpForLevel(126), EXACT);
	}

	@Test
	public void prestigeCostsFollowTheDesignDoc()
	{
		assertEquals(2_107, game.prestigeCost(), EXACT); // Air -> Mind: level 14
		for (int i = 0; i < 4; i++)
		{
			prestigeOnce();
		}
		assertEquals(8_767_248, game.prestigeCost(), EXACT); // Fire -> Body: level 67 x 2^4
		for (int i = 4; i < MAX_TIER; i++)
		{
			prestigeOnce();
		}
		// Wrath -> Ascend: level 126 (the 200M cap) x 2^14
		assertEquals(188_884_740d * 16_384, game.prestigeCost(), EXACT);
	}

	@Test
	public void thereIsAPrestigeLevelForEveryTierAndTheyRise()
	{
		assertEquals(RUNE_NAMES.length, PRESTIGE_LEVELS.length);
		for (int i = 1; i < PRESTIGE_LEVELS.length; i++)
		{
			assertTrue("tier " + i, PRESTIGE_LEVELS[i] > PRESTIGE_LEVELS[i - 1]);
		}
	}

	@Test
	public void ascensionCostsOutpaceAttunement()
	{
		// Otherwise each ascension makes the next run easier than the last
		assertTrue(ASCENSION_COST_FACTOR > 3);
	}

	@Test
	public void prestigeCostsRiseEveryTier()
	{
		double previous = 0;
		for (int i = 0; i <= MAX_TIER; i++)
		{
			assertTrue("tier " + i, game.prestigeCost() > previous);
			previous = game.prestigeCost();
			prestigeOnce();
		}
	}

	@Test
	public void eachAscensionMultipliesPrestigeCosts()
	{
		double firstRun = game.prestigeCost();
		ascendOnce();
		assertEquals(firstRun * ASCENSION_COST_FACTOR, game.prestigeCost(), EXACT);
	}

	@Test
	public void upgradeCostGrowsFifteenPercentPerOwned()
	{
		assertEquals(50, game.upgradeCost(0), EXACT);
		game.gainPoints(50);
		assertEquals(1, game.buyUpgrade(0, false));
		assertEquals(50 * 1.15, game.upgradeCost(0), EXACT);
		assertEquals(0, game.getPoints(), EXACT);
	}

	// ---- Buying ----

	@Test
	public void cannotBuyWhatYouCannotAfford()
	{
		game.gainPoints(49);
		assertEquals(0, game.buyUpgrade(0, false));
		assertFalse(game.buyPouch());
		assertFalse(game.buyDaeyalt());
		assertEquals(49, game.getPoints(), EXACT);
		assertEquals(0, game.getUpgradeCount(0));
	}

	@Test
	public void bulkBuyStopsWhenPointsRunOut()
	{
		// Enough for exactly three talismans: 50 + 57.5 + 66.125
		game.gainPoints(50 + 57.5 + 66.125 + 1);
		assertEquals(3, game.buyUpgrade(0, true));
		assertEquals(3, game.getUpgradeCount(0));
		assertEquals(1, game.getPoints(), 1e-6);
	}

	@Test
	public void bulkBuyIsCappedAtTen()
	{
		game.gainPoints(1e9);
		assertEquals(BULK_BUY_COUNT, game.buyUpgrade(1, true));
	}

	@Test
	public void pouchesAreBoughtInOrderAndRunOut()
	{
		game.gainPoints(1e9);
		for (int i = 0; i < POUCH_NAMES.length; i++)
		{
			assertFalse(game.hasAllPouches());
			assertEquals(POUCH_COSTS[i], game.nextPouchCost(), EXACT);
			assertTrue(game.buyPouch());
		}
		assertTrue(game.hasAllPouches());
		assertFalse(game.buyPouch());
	}

	// ---- Income ----

	@Test
	public void clickValueIsTalismansTimesPouchesTimesTier()
	{
		assertEquals(1, game.click(NOW).gained, EXACT);

		game.gainPoints(1e6);
		game.buyUpgrade(0, false);
		game.buyUpgrade(0, false);
		game.buyPouch();
		// (1 + 2 talismans) x 2^1 pouch
		assertEquals(6, game.pointsPerClick(NOW), EXACT);

		// One prestige doubles it (and resets the talismans and pouch)
		game.gainPoints(game.prestigeCost());
		game.prestige();
		assertEquals(2, game.pointsPerClick(NOW), EXACT);
	}

	@Test
	public void critsMultiplyTheClickByTenAndAreCounted()
	{
		random.nextDouble = 0.0; // below the 5% crit chance
		RuneClickerGame.Click click = game.click(NOW);
		assertTrue(click.crit);
		assertEquals(CRIT_MULTIPLIER, click.gained, EXACT);
		assertEquals(1, game.getCritCount());
		assertEquals(1, game.getTotalClicks());
		assertEquals(CRIT_MULTIPLIER, game.getPoints(), EXACT);
		assertEquals(CRIT_MULTIPLIER, game.getLifetimePoints(), EXACT);
	}

	@Test
	public void idleIncomeIsTheSumOfBuildings()
	{
		game.gainPoints(1e9);
		game.buyUpgrade(1, false); // 0.5/s
		game.buyUpgrade(1, false); // 0.5/s
		game.buyUpgrade(2, false); // 2/s
		game.buyUpgrade(5, false); // 150/s
		assertEquals(153, game.pointsPerSecondExpected(NOW), EXACT);
		assertEquals(153, game.tick(NOW), EXACT);
	}

	@Test
	public void zmiFailsSomeSecondsUnlessPerfected()
	{
		game.gainPoints(1e9);
		game.buyUpgrade(ZMI_INDEX, false);
		assertEquals(30 * 0.9, game.pointsPerSecondExpected(NOW), EXACT);

		random.nextDouble = 0.0; // a failing second
		assertEquals(0, game.tick(NOW), EXACT);
		random.nextDouble = 0.99; // a working second
		assertEquals(30, game.tick(NOW), EXACT);
	}

	@Test
	public void perfectOuraniaNeverFails()
	{
		ascendOnce();
		assertTrue(game.buyPerk(PERK_PERFECT_ZMI));
		game.gainPoints(1e9);
		game.buyUpgrade(ZMI_INDEX, false);
		random.nextDouble = 0.0;
		assertEquals(30, game.tick(NOW), EXACT);
		assertEquals(30, game.pointsPerSecondExpected(NOW), EXACT);
	}

	@Test
	public void daeyaltDoublesIncomeThenGoesOnCooldown()
	{
		assertFalse("not owned yet", game.activateDaeyalt(NOW));
		game.gainPoints(DAEYALT_COST);
		assertTrue(game.buyDaeyalt());
		assertTrue(game.activateDaeyalt(NOW));

		assertEquals(2, game.globalMultiplier(NOW), EXACT);
		long boostOver = NOW + DAEYALT_BOOST_SECONDS * 1000L;
		assertEquals(2, game.globalMultiplier(boostOver - 1), EXACT);
		assertEquals(1, game.globalMultiplier(boostOver), EXACT);

		long cooldownOver = NOW + DAEYALT_COOLDOWN_SECONDS * 1000L;
		assertFalse(game.activateDaeyalt(cooldownOver - 1));
		assertTrue(game.activateDaeyalt(cooldownOver));
	}

	// ---- Golden runes ----

	@Test
	public void goldenJackpotHasAFloorEarlyOn()
	{
		random.nextBoolean = true; // jackpot
		assertEquals(500, game.catchGolden(NOW), EXACT);
		assertEquals(500, game.getPoints(), EXACT);
		assertEquals(1, game.getGoldenCaught());
	}

	@Test
	public void goldenJackpotScalesWithIdleIncome()
	{
		game.gainPoints(1e9);
		game.buyUpgrade(5, false); // 150/s -> 300 s of it = 45,000
		double before = game.getPoints();
		assertEquals(45_000, game.catchGolden(NOW), EXACT);
		assertEquals(before + 45_000, game.getPoints(), EXACT);
	}

	@Test
	public void goldenFrenzyMultipliesIncomeForThirtySeconds()
	{
		random.nextBoolean = false; // frenzy
		assertEquals(0, game.catchGolden(NOW), EXACT);
		assertEquals(FRENZY_MULTIPLIER, game.globalMultiplier(NOW), EXACT);
		assertEquals(1, game.globalMultiplier(NOW + FRENZY_SECONDS * 1000L), EXACT);
	}

	@Test
	public void goldenMagnetHalvesTheWait()
	{
		random.nextInt = 100;
		assertEquals(GOLDEN_MIN_WAIT_S + 100, game.nextGoldenWaitSeconds());
		ascendOnce();
		game.buyPerk(PERK_GOLDEN_MAGNET);
		assertEquals((GOLDEN_MIN_WAIT_S + 100) / 2, game.nextGoldenWaitSeconds());
	}

	// ---- Prestige and ascension ----

	@Test
	public void cannotPrestigeWithoutThePoints()
	{
		game.gainPoints(game.prestigeCost() - 1);
		assertFalse(game.canPrestige());
		assertFalse(game.prestige());
		assertEquals(0, game.getTier());
		assertEquals(0, game.getTotalPrestiges());
	}

	@Test
	public void prestigeResetsTheShopAndDoublesIncome()
	{
		game.gainPoints(1e6);
		game.buyUpgrade(0, true);
		game.buyUpgrade(3, true);
		game.buyPouch();
		game.buyDaeyalt();
		game.activateDaeyalt(NOW);
		double lifetime = game.getLifetimePoints();

		assertTrue(game.prestige());

		assertEquals(1, game.getTier());
		assertEquals(1, game.getTotalPrestiges());
		assertEquals(0, game.getPoints(), EXACT);
		for (int i = 0; i < UPGRADE_NAMES.length; i++)
		{
			assertEquals(0, game.getUpgradeCount(i));
		}
		assertEquals(0, game.getPouchLevel());
		assertFalse(game.isDaeyaltOwned());
		assertFalse(game.isDaeyaltActive(NOW));
		assertEquals("lifetime points survive", lifetime, game.getLifetimePoints(), EXACT);
		assertEquals(2, game.globalMultiplier(NOW), EXACT);
	}

	@Test
	public void ascendingAtWrathGoesBackToAirWithARunespanPoint()
	{
		for (int i = 0; i < MAX_TIER; i++)
		{
			prestigeOnce();
		}
		assertEquals(MAX_TIER, game.getTier());
		assertTrue(game.isAscensionNext());

		prestigeOnce();

		assertEquals(0, game.getTier());
		assertEquals(1, game.getAscensions());
		assertEquals(1, game.unspentRunespanPoints());
		assertEquals(MAX_TIER + 1, game.getTotalPrestiges());
		assertEquals(1, game.globalMultiplier(NOW), EXACT);
	}

	@Test
	public void perksCostRunespanPointsAndOneOffsCannotBeRebought()
	{
		assertFalse("no points before ascending", game.buyPerk(PERK_ATTUNEMENT));
		ascendOnce();
		ascendOnce();
		assertTrue(game.buyPerk(PERK_POUCH_KEEPER));
		assertTrue(game.isPerkMaxed(PERK_POUCH_KEEPER));
		assertFalse(game.buyPerk(PERK_POUCH_KEEPER));
		assertEquals(1, game.unspentRunespanPoints());
		assertTrue(game.buyPerk(PERK_ATTUNEMENT));
		assertFalse("out of points", game.buyPerk(PERK_ATTUNEMENT));
	}

	@Test
	public void attunementTriplesIncomeAndStacks()
	{
		ascendOnce();
		ascendOnce();
		game.buyPerk(PERK_ATTUNEMENT);
		game.buyPerk(PERK_ATTUNEMENT);
		assertEquals(9, game.globalMultiplier(NOW), EXACT);
	}

	@Test
	public void pouchKeeperKeepsPouchesOnPrestigeButNotOnAscension()
	{
		ascendOnce();
		game.buyPerk(PERK_POUCH_KEEPER);
		game.gainPoints(1e6);
		game.buyPouch();
		game.buyPouch();

		prestigeOnce();
		assertEquals(2, game.getPouchLevel());

		for (int i = game.getTier(); i <= MAX_TIER; i++)
		{
			prestigeOnce();
		}
		assertEquals(2, game.getAscensions());
		assertEquals(0, game.getPouchLevel());
	}

	@Test
	public void wizardFinixClicksOncePerSecondPerLevel()
	{
		ascendOnce();
		ascendOnce();
		game.buyPerk(PERK_AUTO_CLICK);
		game.buyPerk(PERK_AUTO_CLICK);
		assertEquals(2 * game.pointsPerClick(NOW), game.tick(NOW), EXACT);
	}

	// ---- Saving and loading ----

	@Test
	public void saveThenLoadGivesTheSameGame()
	{
		ascendOnce();
		game.buyPerk(PERK_ATTUNEMENT);
		prestigeOnce();
		game.gainPoints(123_456_789.25);
		game.buyUpgrade(0, true);
		game.buyUpgrade(4, false);
		game.buyPouch();
		game.buyDaeyalt();
		game.click(NOW);
		game.goldenAppeared();
		game.goldenAppeared();
		game.catchGolden(NOW);
		random.nextDouble = 0.0;
		game.click(NOW);

		String saved = game.serialize();
		RuneClickerGame loaded = new RuneClickerGame(new FixedRandom());
		assertTrue(loaded.load(saved));

		assertEquals(saved, loaded.serialize());
		assertEquals(game.getTier(), loaded.getTier());
		assertEquals(game.getPoints(), loaded.getPoints(), EXACT);
		assertEquals(game.getLifetimePoints(), loaded.getLifetimePoints(), EXACT);
		assertEquals(game.getAscensions(), loaded.getAscensions());
		assertEquals(game.getPouchLevel(), loaded.getPouchLevel());
		assertTrue(loaded.isDaeyaltOwned());
		assertEquals(10, loaded.getUpgradeCount(0));
		assertEquals(1, loaded.getUpgradeCount(4));
		assertEquals(1, loaded.getPerkCount(PERK_ATTUNEMENT));
		assertEquals(2, loaded.getTotalClicks());
		assertEquals(2, loaded.getGoldenSeen());
		assertEquals(1, loaded.getGoldenCaught());
		assertEquals(1, loaded.getCritCount());
		assertEquals(game.getTotalPrestiges(), loaded.getTotalPrestiges());
	}

	@Test
	public void hugePointTotalsSurviveASave()
	{
		game.gainPoints(1.5e40);
		RuneClickerGame loaded = new RuneClickerGame(new FixedRandom());
		assertTrue(loaded.load(game.serialize()));
		assertEquals(1.5e40, loaded.getPoints(), 1e25);
	}

	@Test
	public void loadsVersion1Saves()
	{
		assertTrue(game.load("1|3|1234.5|0|2|1|4,5,0,0,0,0"));
		assertEquals(3, game.getTier());
		assertEquals(1234.5, game.getPoints(), EXACT);
		assertEquals(2, game.getPouchLevel());
		assertTrue(game.isDaeyaltOwned());
		assertEquals(4, game.getUpgradeCount(0));
		assertEquals(5, game.getUpgradeCount(1));
		// Fields v1 didn't have get sensible defaults
		assertEquals(1234.5, game.getLifetimePoints(), EXACT);
		assertEquals(3, game.getTotalPrestiges());
		assertEquals(0, game.getPerkCount(PERK_ATTUNEMENT));
		assertEquals(0, game.getGoldenSeen());
		assertEquals(0, game.getCritCount());
	}

	@Test
	public void loadsVersion2Saves()
	{
		assertTrue(game.load("2|1|10.0|1|0|0|1,2,3,4,5,6|999.0|42|16|0|1,0,0,0,1"));
		assertEquals(1, game.getAscensions());
		assertEquals(999.0, game.getLifetimePoints(), EXACT);
		assertEquals(42, game.getTotalClicks());
		assertEquals(16, game.getTotalPrestiges());
		assertEquals(1, game.getPerkCount(PERK_AUTO_CLICK));
		assertEquals(1, game.getPerkCount(PERK_ATTUNEMENT));
		assertEquals(0, game.getGoldenSeen());
		assertEquals(0, game.getGoldenCaught());
		assertEquals(0, game.getCritCount());
	}

	@Test
	public void loadsVersion3Saves()
	{
		assertTrue(game.load("3|1|10.0|1|0|0|1,2,3,4,5,6|999.0|42|16|0|1,0,0,0,1|7|5"));
		assertEquals(7, game.getGoldenSeen());
		assertEquals(5, game.getGoldenCaught());
		assertEquals(0, game.getCritCount());
	}

	@Test
	public void savesWithFewerBuildingsThanTodayStillLoad()
	{
		// When new buildings are added, older saves list fewer counts
		assertTrue(game.load("4|0|0.0|0|0|0|1,2|0.0|0|0|0|0|0|0|0"));
		assertEquals(1, game.getUpgradeCount(0));
		assertEquals(2, game.getUpgradeCount(1));
		assertEquals(0, game.getUpgradeCount(5));
	}

	@Test
	public void corruptSavesStartFreshInsteadOfCrashing()
	{
		for (String bad : new String[]{"", "garbage", "4|x|1.0", "4|1|2.0|0|0|0", "4|1|abc|0|0|0|0,0,0,0,0,0|0|0|0|0|0|0|0|0"})
		{
			game.gainPoints(100);
			assertFalse(bad, game.load(bad));
			assertFalse(bad, game.hasProgress());
			assertEquals(bad, 0, game.getPoints(), EXACT);
		}
	}

	@Test
	public void resetWipesEverything()
	{
		ascendOnce();
		game.buyPerk(PERK_ATTUNEMENT);
		game.gainPoints(500);
		game.click(NOW);
		assertTrue(game.hasProgress());

		game.reset();

		assertFalse(game.hasProgress());
		assertEquals(0, game.getAscensions());
		assertEquals(0, game.getPerkCount(PERK_ATTUNEMENT));
		assertEquals(new RuneClickerGame(new FixedRandom()).serialize(), game.serialize());
	}

	@Test
	public void cookieMigrationStartsWithThosePoints()
	{
		game.resetWithStartingPoints(250);
		assertEquals(250, game.getPoints(), EXACT);
		assertEquals(250, game.getLifetimePoints(), EXACT);
		assertTrue(game.hasProgress());
	}
}
