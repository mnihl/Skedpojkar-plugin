package com.skedpojkar.panel;

import com.skedpojkar.SkedpojkarConfig;
import com.skedpojkar.achievements.Achievement;
import com.skedpojkar.achievements.AchievementManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Runecrafting clicker (replaces the old cookie clicker; existing cookie
 * counts migrate into starting points once, per character).
 *
 * Click the rune to craft (small crit chance); buy upgrades that craft for
 * you; catch golden runes when they appear; prestige through the real rune
 * tiers (Air → Wrath, each doubling ALL income); after Wrath, Ascend for
 * Runespan points spent on permanent perks. Milestones print chat messages.
 * Idle income only accrues while the client is open and you're logged in.
 * All numbers are first-pass balance — tune freely (see RUNECLICKER_DESIGN.md).
 */
@Slf4j
public class RuneClickerPanel extends JPanel
{
	private static final String STATE_KEY = "runeClicker";
	private static final String LEGACY_COOKIE_KEY = "cookieCount";
	private static final int STATE_VERSION = 4;

	private static final int BULK_BUY_COUNT = 10;

	private static final String[] RUNE_NAMES = {
		"Air", "Mind", "Water", "Earth", "Fire", "Body", "Cosmic", "Chaos",
		"Astral", "Nature", "Law", "Death", "Blood", "Soul", "Wrath",
	};
	private static final int[] RUNE_ITEM_IDS = {
		ItemID.AIR_RUNE, ItemID.MIND_RUNE, ItemID.WATER_RUNE, ItemID.EARTH_RUNE,
		ItemID.FIRE_RUNE, ItemID.BODY_RUNE, ItemID.COSMIC_RUNE, ItemID.CHAOS_RUNE,
		ItemID.ASTRAL_RUNE, ItemID.NATURE_RUNE, ItemID.LAW_RUNE, ItemID.DEATH_RUNE,
		ItemID.BLOOD_RUNE, ItemID.SOUL_RUNE, ItemID.WRATH_RUNE,
	};
	private static final int MAX_TIER = RUNE_NAMES.length - 1;

	// Repeatable shop upgrades: cost = base * 1.15^owned. Index 0 adds to
	// points per click; the rest generate points per second. The talisman is
	// named after the current tier's rune ("Mind talisman", ...).
	private static final String[] UPGRADE_NAMES = {
		"talisman", "Rune essence miner", "Pure essence miner",
		"Abyssal leech", "ZMI altar trips", "Wicked hood",
	};
	private static final double[] UPGRADE_BASE_COSTS = {50, 200, 1_500, 10_000, 75_000, 400_000};
	private static final double[] UPGRADE_IDLE_RATES = {0, 0.5, 2, 8, 30, 150};
	private static final int ZMI_INDEX = 4;
	private static final double ZMI_FAIL_CHANCE = 0.10;

	// One-off pouch ladder: each stage doubles points per click
	private static final String[] POUCH_NAMES = {"Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch"};
	private static final double[] POUCH_COSTS = {500, 5_000, 50_000, 500_000, 5_000_000};

	// Daeyalt shard infusion: one-off purchase, then an activatable x2 boost
	private static final double DAEYALT_COST = 25_000;
	private static final int DAEYALT_BOOST_SECONDS = 60;
	private static final int DAEYALT_COOLDOWN_SECONDS = 300;

	// Clicks have a small chance to crit for x10
	private static final double CRIT_CHANCE = 0.05;
	private static final int CRIT_MULTIPLIER = 10;

	// Golden rune: appears at a random interval, stays clickable briefly.
	// Reward is a points jackpot or a temporary x7 frenzy, 50/50.
	private static final int GOLDEN_MIN_WAIT_S = 180;
	private static final int GOLDEN_EXTRA_WAIT_S = 420;
	private static final int GOLDEN_VISIBLE_S = 10;
	private static final int FRENZY_SECONDS = 30;
	private static final int FRENZY_MULTIPLIER = 7;

	// Runespan perks, bought with points earned by ascending (1 each).
	// AUTO_CLICK and ATTUNEMENT are repeatable; the rest are one-offs.
	private static final int PERK_AUTO_CLICK = 0;
	private static final int PERK_POUCH_KEEPER = 1;
	private static final int PERK_PERFECT_ZMI = 2;
	private static final int PERK_GOLDEN_MAGNET = 3;
	private static final int PERK_ATTUNEMENT = 4;
	private static final String[] PERK_NAMES = {
		"Wizard Finix's help", "Pouch keeper", "Perfect Ourania", "Golden magnetism", "Runespan attunement",
	};
	private static final String[] PERK_DESCRIPTIONS = {
		"Auto-clicks the rune once per second per level (full click value).",
		"Pouches survive prestige.",
		"ZMI altar trips never fail.",
		"Golden runes appear twice as often.",
		"All income x3, permanently. Stacks.",
	};
	private static final boolean[] PERK_REPEATABLE = {true, false, false, false, true};

	private static final int NATURE_TIER = 9;

	private static final int AUTOSAVE_EVERY_TICKS = 10;
	private static final long CLICK_FEEDBACK_MS = 1_500;

	// Shop row icons (item sprites). Some are stand-ins: the Wicked hood and
	// Wizard Finix are RS3 flavor with no OSRS item, so mystic hat / air staff.
	private static final int[] UPGRADE_ICON_IDS = {
		ItemID.AIR_TALISMAN, ItemID.RUNE_ESSENCE, ItemID.PURE_ESSENCE,
		ItemID.ABYSSAL_WHIP, ItemID.LAW_RUNE, ItemID.MYSTIC_HAT,
	};
	private static final int[] POUCH_ICON_IDS = {
		ItemID.SMALL_POUCH, ItemID.MEDIUM_POUCH, ItemID.LARGE_POUCH,
		ItemID.GIANT_POUCH, ItemID.COLOSSAL_POUCH,
	};
	private static final int[] PERK_ICON_IDS = {
		ItemID.STAFF_OF_AIR, ItemID.GIANT_POUCH, ItemID.ASTRAL_RUNE,
		ItemID.GOLDEN_NUGGET, ItemID.SOUL_RUNE,
	};
	private static final int SHOP_ICON_SIZE = 22;

	// The talisman row's icon follows the tier. Not every rune has a talisman
	// in OSRS (astral/blood/soul/wrath) — the elemental talisman stands in.
	private static final int[] TALISMAN_ICON_IDS = {
		ItemID.AIR_TALISMAN, ItemID.MIND_TALISMAN, ItemID.WATER_TALISMAN,
		ItemID.EARTH_TALISMAN, ItemID.FIRE_TALISMAN, ItemID.BODY_TALISMAN,
		ItemID.COSMIC_TALISMAN, ItemID.CHAOS_TALISMAN, ItemID.ELEMENTAL_TALISMAN,
		ItemID.NATURE_TALISMAN, ItemID.LAW_TALISMAN, ItemID.DEATH_TALISMAN,
		ItemID.ELEMENTAL_TALISMAN, ItemID.ELEMENTAL_TALISMAN, ItemID.ELEMENTAL_TALISMAN,
	};

	// The clickable rune is drawn larger than the native 36x32 item sprite
	private static final int RUNE_SIZE = 76;

	private final SkedpojkarConfig config;
	private final ConfigManager configManager;
	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final AchievementManager achievements;
	private final Random random = new Random();

	// Game state (persisted per character)
	private int tier;
	private double points;
	private int ascensions;
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
	private final int[] perkCounts = new int[PERK_NAMES.length];

	// Transient state
	private boolean loggedIn;
	private long daeyaltBoostEndMs;
	private long daeyaltCooldownEndMs;
	private long frenzyEndMs;
	private long nextGoldenAtMs;
	private long goldenVisibleUntilMs;
	private long clickFeedbackUntilMs;
	private int ticksSinceSave;

	// UI
	private final RuneButton runeButton = new RuneButton();
	private final ConfettiPane confettiPane = new ConfettiPane();
	private final JButton goldenButton = new JButton("Golden rune!");
	private final JLabel pointsLabel = new JLabel("", SwingConstants.CENTER);
	private final JLabel rateLabel = new JLabel("", SwingConstants.CENTER);
	private final JLabel feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
	private final JButton[] upgradeButtons = new JButton[UPGRADE_NAMES.length];
	private final JLabel[] upgradeLabels = new JLabel[UPGRADE_NAMES.length];
	private final JLabel[] upgradeIconLabels = new JLabel[UPGRADE_NAMES.length];
	private int talismanIconTier = -1;
	private final JButton pouchButton = new JButton("Buy");
	private final JLabel pouchLabel = new JLabel();
	private final JLabel pouchIconLabel = new JLabel();
	private int pouchIconLevel = -1;
	private final JButton daeyaltButton = new JButton();
	private final JLabel daeyaltLabel = new JLabel();
	private final JButton prestigeButton = new JButton();
	private final JLabel perkHeaderLabel = new JLabel();
	private final JButton[] perkButtons = new JButton[PERK_NAMES.length];
	private final JLabel[] perkLabels = new JLabel[PERK_NAMES.length];
	private final JPanel[] perkRows = new JPanel[PERK_NAMES.length];
	private final JLabel statsLabel = new JLabel();
	private final JLabel achievementsHeader = new JLabel();
	private final JLabel[] achievementLabels = new JLabel[Achievement.values().length];
	private boolean achievementsExpanded;

	public RuneClickerPanel(SkedpojkarConfig config, ConfigManager configManager, Client client,
		ClientThread clientThread, ItemManager itemManager, AchievementManager achievements)
	{
		this.config = config;
		this.configManager = configManager;
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.achievements = achievements;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Top: the rune to click, counters, prestige, and the golden rune spot.
		// The rune is a custom-painted component: bare sprite, smoothly animated
		// scale (hover grow, press dip), always centered.
		pointsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		rateLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		feedbackLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		goldenButton.setBackground(ColorScheme.BRAND_ORANGE);
		goldenButton.setFocusPainted(false);
		goldenButton.setVisible(false);
		goldenButton.addActionListener(e -> onGoldenClicked());
		AsyncBufferedImage goldenImg = itemManager.getImage(ItemID.GOLDEN_NUGGET);
		goldenButton.setIcon(new ImageIcon(goldenImg));
		goldenImg.onLoaded(() ->
		{
			goldenButton.setIcon(new ImageIcon(goldenImg));
			goldenButton.repaint();
		});

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);
		confettiPane.add(runeButton);
		for (Component c : new Component[]{confettiPane, feedbackLabel, pointsLabel, rateLabel, prestigeButton, goldenButton})
		{
			((javax.swing.JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
		}
		top.add(confettiPane);
		top.add(feedbackLabel);
		top.add(pointsLabel);
		top.add(rateLabel);
		top.add(Box.createVerticalStrut(6));
		top.add(prestigeButton);
		top.add(Box.createVerticalStrut(4));
		top.add(goldenButton);
		prestigeButton.addActionListener(e -> onPrestige());
		// Cap widths: the fixed ~225px sidebar must never be widened (that
		// resizes the client window). The prestige button is two lines tall so
		// even huge full-digit costs stay legible.
		prestigeButton.setPreferredSize(new Dimension(200, 42));
		prestigeButton.setMaximumSize(new Dimension(200, 42));
		goldenButton.setMaximumSize(new Dimension(200, 34));

		// Middle: the shop
		JPanel shop = new JPanel();
		shop.setLayout(new BoxLayout(shop, BoxLayout.Y_AXIS));
		shop.setOpaque(false);
		for (int i = 0; i < UPGRADE_NAMES.length; i++)
		{
			final int idx = i;
			upgradeButtons[i] = new JButton("Buy");
			upgradeButtons[i].setToolTipText("Shift-click: buy up to " + BULK_BUY_COUNT);
			upgradeButtons[i].addActionListener(e ->
				buyUpgrade(idx, (e.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0));
			upgradeLabels[i] = new JLabel();
			upgradeIconLabels[i] = itemIconLabel(UPGRADE_ICON_IDS[i]);
			shop.add(shopRow(upgradeIconLabels[i], upgradeLabels[i], upgradeButtons[i]));
		}
		pouchIconLabel.setPreferredSize(new Dimension(SHOP_ICON_SIZE + 2, SHOP_ICON_SIZE + 2));
		pouchButton.addActionListener(e -> buyPouch());
		shop.add(shopRow(pouchIconLabel, pouchLabel, pouchButton));
		daeyaltButton.addActionListener(e -> onDaeyaltButton());
		shop.add(shopRow(itemIconLabel(ItemID.DAEYALT_SHARD), daeyaltLabel, daeyaltButton));

		// Runespan perks (hidden until the first ascension)
		perkHeaderLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		perkHeaderLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
		perkHeaderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		shop.add(perkHeaderLabel);
		for (int i = 0; i < PERK_NAMES.length; i++)
		{
			final int idx = i;
			perkButtons[i] = new JButton("Buy");
			perkButtons[i].addActionListener(e -> buyPerk(idx));
			perkLabels[i] = new JLabel();
			perkRows[i] = shopRow(itemIconLabel(PERK_ICON_IDS[i]), perkLabels[i], perkButtons[i]);
			shop.add(perkRows[i]);
		}

		// Stats at the bottom of the shop
		statsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		statsLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		shop.add(statsLabel);

		// Achievements list (plugin-wide, viewed here). Collapsed by default;
		// the header is clickable to expand/collapse.
		achievementsHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		achievementsHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
		achievementsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		achievementsHeader.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		setItemIcon(achievementsHeader, ItemID.CHAMPION_SCROLL);
		achievementsHeader.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				achievementsExpanded = !achievementsExpanded;
				refreshAchievements();
			}
		});
		shop.add(achievementsHeader);
		Achievement[] all = Achievement.values();
		for (int i = 0; i < all.length; i++)
		{
			achievementLabels[i] = new JLabel();
			achievementLabels[i].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
			achievementLabels[i].setAlignmentX(Component.LEFT_ALIGNMENT);
			achievementLabels[i].setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
			shop.add(achievementLabels[i]);
		}

		// Destructive reset at the very bottom, out of the way
		JButton resetButton = new JButton("Reset progress");
		resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		resetButton.setToolTipText("Wipe this character's Runeclicker progress (achievements kept)");
		resetButton.addActionListener(e -> onResetProgress());
		JPanel resetWrap = new JPanel(new BorderLayout());
		resetWrap.setOpaque(false);
		resetWrap.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
		resetWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		resetWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
		resetWrap.add(resetButton, BorderLayout.WEST);
		shop.add(resetWrap);

		JScrollPane scroll = new JScrollPane(shop,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());

		add(top, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		scheduleNextGolden();
		refresh();

		// 1 s game tick: idle income, timers, UI refresh, throttled saves.
		// Swing timers only fire while the client runs = no offline progress.
		Timer timer = new Timer(1000, e -> tick());
		timer.start();
	}

	private JPanel shopRow(JLabel iconLabel, JLabel label, JButton button)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
		label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		button.setMargin(new Insets(2, 6, 2, 6));
		if (iconLabel != null)
		{
			row.add(iconLabel, BorderLayout.WEST);
		}
		row.add(label, BorderLayout.CENTER);
		row.add(button, BorderLayout.EAST);
		return row;
	}

	/** A small item-sprite label for a shop row. */
	private JLabel itemIconLabel(int itemId)
	{
		JLabel label = new JLabel();
		label.setPreferredSize(new Dimension(SHOP_ICON_SIZE + 2, SHOP_ICON_SIZE + 2));
		setItemIcon(label, itemId);
		return label;
	}

	private void setItemIcon(JLabel target, int itemId)
	{
		AsyncBufferedImage img = itemManager.getImage(itemId);
		Runnable apply = () ->
		{
			target.setIcon(new ImageIcon(img.getScaledInstance(
				SHOP_ICON_SIZE, SHOP_ICON_SIZE, java.awt.Image.SCALE_SMOOTH)));
			target.repaint();
		};
		apply.run();
		img.onLoaded(apply);
	}

	/** The RS profile key whose state is currently loaded in memory. */
	private String loadedProfileKey;

	/** The key whose in-memory state represents real progress (guards against
	 * a transient null read wiping a character during login). */
	private String stateOwnerKey;

	/**
	 * True while actually in the game world. The profile key alone is NOT a
	 * login check — RuneLite keeps the last character's key after logout, so
	 * the game state must be consulted too. LOADING counts as playing (it
	 * flickers on every region crossing mid-session).
	 */
	private boolean isPlaying()
	{
		net.runelite.api.GameState gs = client.getGameState();
		return (gs == GameState.LOGGED_IN || gs == GameState.LOADING)
			&& configManager.getRSProfileKey() != null;
	}

	/** Reloads state for the current character. Called on login/account switch. */
	public void refresh()
	{
		loggedIn = isPlaying();
		if (loggedIn)
		{
			loadedProfileKey = configManager.getRSProfileKey();
			loadState();
		}
		refreshUi();
	}

	// ---- Game logic ----

	/**
	 * Applies to ALL income, clicks and idle alike: x2 per rune tier, x3 per
	 * Runespan attunement perk, x2 during Daeyalt, x7 during a golden frenzy.
	 */
	private double globalMultiplier()
	{
		double mult = Math.pow(2, tier) * Math.pow(3, perkCounts[PERK_ATTUNEMENT]);
		long now = System.currentTimeMillis();
		if (now < daeyaltBoostEndMs)
		{
			mult *= 2;
		}
		if (now < frenzyEndMs)
		{
			mult *= FRENZY_MULTIPLIER;
		}
		return mult;
	}

	private double pointsPerClick()
	{
		double base = 1 + upgradeCounts[0];
		return base * Math.pow(2, pouchLevel) * globalMultiplier();
	}

	private double zmiYieldFactor()
	{
		return perkCounts[PERK_PERFECT_ZMI] > 0 ? 1 : 1 - ZMI_FAIL_CHANCE;
	}

	/** Expected idle income per second (ZMI at its average yield; auto-clicks included). */
	private double pointsPerSecondExpected()
	{
		double sum = 0;
		for (int i = 1; i < UPGRADE_NAMES.length; i++)
		{
			double rate = UPGRADE_IDLE_RATES[i] * upgradeCounts[i];
			if (i == ZMI_INDEX)
			{
				rate *= zmiYieldFactor();
			}
			sum += rate;
		}
		return sum * globalMultiplier() + perkCounts[PERK_AUTO_CLICK] * pointsPerClick();
	}

	private double upgradeCost(int idx)
	{
		return UPGRADE_BASE_COSTS[idx] * Math.pow(1.15, upgradeCounts[idx]);
	}

	private static final int PRESTIGE_MIN_LEVEL = 20;
	private static final int PRESTIGE_MAX_LEVEL = 99;

	/**
	 * Prestige cost follows the real OSRS experience curve: the 15 tiers map
	 * evenly across levels 20 → 99, so the cost is the total XP for that level.
	 * The first prestige is ~4,470 (level 20) and the final Ascend is
	 * 13,034,431 (level 99) — a real grind, but a familiar and achievable one,
	 * not an abstract quintillion.
	 */
	private double prestigeCost()
	{
		int level = (int) Math.round(PRESTIGE_MIN_LEVEL
			+ tier * (PRESTIGE_MAX_LEVEL - PRESTIGE_MIN_LEVEL) / (double) MAX_TIER);
		return xpForLevel(level);
	}

	/** Total OSRS experience required to reach the given level. */
	private static double xpForLevel(int level)
	{
		double points = 0;
		for (int lvl = 1; lvl < level; lvl++)
		{
			points += Math.floor(lvl + 300 * Math.pow(2, lvl / 7.0));
		}
		return Math.floor(points / 4);
	}

	private int unspentRunespanPoints()
	{
		int spent = 0;
		for (int c : perkCounts)
		{
			spent += c;
		}
		return ascensions - spent;
	}

	private void gainPoints(double amount)
	{
		points += amount;
		lifetimePoints += amount;
	}

	private void onRuneClicked()
	{
		if (!loggedIn)
		{
			return;
		}
		totalClicks++;
		double gained = pointsPerClick();
		boolean crit = random.nextDouble() < CRIT_CHANCE;
		if (crit)
		{
			gained *= CRIT_MULTIPLIER;
			critCount++;
		}
		gainPoints(gained);
		if (crit)
		{
			confettiPane.burst();
		}
		showFeedback(crit ? "CRIT! +" + format(gained) : "+" + format(gained),
			crit ? ColorScheme.BRAND_ORANGE : null);
		checkMilestones();
		refreshUi();
	}

	private void onGoldenClicked()
	{
		goldenButton.setVisible(false);
		goldenVisibleUntilMs = 0;
		scheduleNextGolden();
		if (!loggedIn)
		{
			return;
		}
		goldenCaught++;

		if (random.nextBoolean())
		{
			// Jackpot: a solid chunk relative to your economy
			double jackpot = Math.max(pointsPerClick() * 50, pointsPerSecondExpected() * 300);
			jackpot = Math.max(jackpot, 500); // floor for early game
			gainPoints(jackpot);
			showFeedback("Golden rune! +" + format(jackpot), ColorScheme.BRAND_ORANGE);
		}
		else
		{
			frenzyEndMs = System.currentTimeMillis() + FRENZY_SECONDS * 1000L;
			showFeedback("FRENZY! x" + FRENZY_MULTIPLIER + " for " + FRENZY_SECONDS + " s", ColorScheme.BRAND_ORANGE);
		}
		checkMilestones();
		saveState();
		refreshUi();
	}

	private void scheduleNextGolden()
	{
		int wait = GOLDEN_MIN_WAIT_S + random.nextInt(GOLDEN_EXTRA_WAIT_S);
		if (perkCounts[PERK_GOLDEN_MAGNET] > 0)
		{
			wait /= 2;
		}
		nextGoldenAtMs = System.currentTimeMillis() + wait * 1000L;
	}

	private void buyUpgrade(int idx, boolean bulk)
	{
		if (!loggedIn)
		{
			return;
		}
		int toBuy = bulk ? BULK_BUY_COUNT : 1;
		boolean bought = false;
		for (int i = 0; i < toBuy; i++)
		{
			double cost = upgradeCost(idx);
			if (points < cost)
			{
				break;
			}
			points -= cost;
			upgradeCounts[idx]++;
			bought = true;
		}
		if (bought)
		{
			saveState();
			refreshUi();
		}
	}

	private void buyPouch()
	{
		if (!loggedIn || pouchLevel >= POUCH_NAMES.length || points < POUCH_COSTS[pouchLevel])
		{
			return;
		}
		points -= POUCH_COSTS[pouchLevel];
		pouchLevel++;
		saveState();
		refreshUi();
	}

	private void buyPerk(int idx)
	{
		if (!loggedIn || unspentRunespanPoints() < 1
			|| (!PERK_REPEATABLE[idx] && perkCounts[idx] > 0))
		{
			return;
		}
		perkCounts[idx]++;
		saveState();
		refreshUi();
	}

	private void onDaeyaltButton()
	{
		if (!loggedIn)
		{
			return;
		}
		long now = System.currentTimeMillis();
		if (!daeyaltOwned)
		{
			if (points < DAEYALT_COST)
			{
				return;
			}
			points -= DAEYALT_COST;
			daeyaltOwned = true;
			saveState();
		}
		else if (now >= daeyaltCooldownEndMs)
		{
			daeyaltBoostEndMs = now + DAEYALT_BOOST_SECONDS * 1000L;
			daeyaltCooldownEndMs = now + DAEYALT_COOLDOWN_SECONDS * 1000L;
		}
		refreshUi();
	}

	private void onPrestige()
	{
		if (!loggedIn || points < prestigeCost())
		{
			return;
		}

		boolean ascend = tier >= MAX_TIER;
		boolean keepPouches = perkCounts[PERK_POUCH_KEEPER] > 0;
		String warning = ascend
			? "Ascend the Runespan? EVERYTHING resets (back to Air runes), but you gain 1 Runespan point to spend on permanent perks."
			: "Prestige to " + RUNE_NAMES[tier + 1] + " runes? Points and upgrades reset"
			+ (keepPouches ? " (pouches kept)" : "") + "; ALL income doubles permanently.";
		if (JOptionPane.showConfirmDialog(this, warning, "Are you sure?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
		{
			return;
		}

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
		checkMilestones();
		saveState();
		refreshUi();
	}

	private void tick()
	{
		// Re-check every tick: logging out must stop the clock (no
		// offline/logged-out progress), and logging in must load the
		// character's state before any income accrues.
		if (!isPlaying())
		{
			if (loggedIn)
			{
				// Save the last few seconds while the profile key still
				// belongs to the character who earned them
				saveState();
				loggedIn = false;
				goldenButton.setVisible(false);
				refreshUi();
			}
			return;
		}
		if (!loggedIn)
		{
			refresh();
			return;
		}

		double income = 0;
		for (int i = 1; i < UPGRADE_NAMES.length; i++)
		{
			double rate = UPGRADE_IDLE_RATES[i] * upgradeCounts[i];
			if (i == ZMI_INDEX && perkCounts[PERK_PERFECT_ZMI] == 0
				&& random.nextDouble() < ZMI_FAIL_CHANCE)
			{
				rate = 0; // it's random, it's ZMI
			}
			income += rate;
		}
		income *= globalMultiplier();
		// Wizard Finix clicks for you (full click value, crits not included)
		income += perkCounts[PERK_AUTO_CLICK] * pointsPerClick();
		gainPoints(income);

		// Golden rune appearance/expiry
		long now = System.currentTimeMillis();
		if (goldenButton.isVisible() && now > goldenVisibleUntilMs)
		{
			goldenButton.setVisible(false);
			scheduleNextGolden();
		}
		else if (!goldenButton.isVisible() && now >= nextGoldenAtMs)
		{
			goldenVisibleUntilMs = now + GOLDEN_VISIBLE_S * 1000L;
			goldenButton.setVisible(true);
			goldenSeen++;
		}

		if (now > clickFeedbackUntilMs)
		{
			feedbackLabel.setText(" ");
		}

		checkMilestones();
		if (++ticksSinceSave >= AUTOSAVE_EVERY_TICKS)
		{
			saveState();
		}
		refreshUi();
	}

	// ---- Achievements (chat announcements handled by AchievementManager) ----

	private void checkMilestones()
	{
		// Don't evaluate while the game is loading — unlock announcements
		// would be dropped, and profile writes may not resolve
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (lifetimePoints >= 10_000)
		{
			achievements.unlock(Achievement.POINTS_10K);
		}
		if (lifetimePoints >= 1_000_000)
		{
			achievements.unlock(Achievement.POINTS_1M);
		}
		if (lifetimePoints >= 1_000_000_000)
		{
			achievements.unlock(Achievement.POINTS_1B);
		}
		if (lifetimePoints >= 1_000_000_000_000L)
		{
			achievements.unlock(Achievement.POINTS_1T);
		}
		if (totalClicks >= 1_000)
		{
			achievements.unlock(Achievement.CLICKS_1K);
		}
		if (totalPrestiges >= 1)
		{
			achievements.unlock(Achievement.FIRST_PRESTIGE);
		}
		if (tier >= NATURE_TIER)
		{
			achievements.unlock(Achievement.NATURE_TIER);
		}
		if (tier >= MAX_TIER)
		{
			achievements.unlock(Achievement.WRATH_TIER);
		}
		if (ascensions >= 1)
		{
			achievements.unlock(Achievement.FIRST_ASCENSION);
		}
		if (goldenCaught >= 10)
		{
			achievements.unlock(Achievement.GOLDEN_10);
		}
		if (goldenCaught >= 50)
		{
			achievements.unlock(Achievement.GOLDEN_50);
		}
		if (goldenSeen - goldenCaught >= 10)
		{
			achievements.unlock(Achievement.BUTTERFINGERS);
		}
		if (critCount >= 100)
		{
			achievements.unlock(Achievement.CRIT_100);
		}
		if (pouchLevel >= POUCH_NAMES.length)
		{
			achievements.unlock(Achievement.MAXED_POUCHES);
		}
		for (int count : upgradeCounts)
		{
			if (count >= 50)
			{
				achievements.unlock(Achievement.BIG_SPENDER);
				break;
			}
		}
		if (perkCounts[PERK_ATTUNEMENT] >= 3)
		{
			achievements.unlock(Achievement.FULLY_ATTUNED);
		}
	}

	// ---- Persistence ----

	private void loadState()
	{
		String stored = configManager.getRSProfileConfiguration(SkedpojkarConfig.GROUP, STATE_KEY);
		if (stored == null)
		{
			// A missing save for a profile we already hold real progress for is
			// almost certainly a transient read failure during login — keep the
			// in-memory state instead of wiping it with a fresh one
			if (loadedProfileKey != null && loadedProfileKey.equals(stateOwnerKey)
				&& (lifetimePoints > 0 || tier > 0 || totalClicks > 0))
			{
				log.warn("Saved Runeclicker state missing for profile {} but in-memory progress exists — keeping it", loadedProfileKey);
				saveState();
				return;
			}
			// INFO on purpose: fresh-state initialization is the prime suspect in
			// "my progress was wiped" reports — this line makes them diagnosable
			log.info("No saved Runeclicker state for profile {}, starting fresh", loadedProfileKey);
			migrateFromCookies();
			stateOwnerKey = loadedProfileKey;
			return;
		}

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
			stateOwnerKey = loadedProfileKey;
		}
		catch (Exception e)
		{
			log.warn("Corrupt rune clicker state, starting fresh: {}", stored, e);
			resetAll();
			stateOwnerKey = loadedProfileKey;
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

	/** One-time migration: old cookie counts become starting points. */
	private void migrateFromCookies()
	{
		resetAll();
		String cookies = configManager.getRSProfileConfiguration(SkedpojkarConfig.GROUP, LEGACY_COOKIE_KEY);
		if (cookies == null)
		{
			cookies = configManager.getConfiguration(SkedpojkarConfig.GROUP, LEGACY_COOKIE_KEY);
		}
		try
		{
			points = cookies == null ? 0 : Long.parseLong(cookies);
		}
		catch (NumberFormatException e)
		{
			points = 0;
		}
		lifetimePoints = points;
		saveState();
	}

	private void resetAll()
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

	/** Wipes this character's clicker progress after a double confirmation. */
	private void onResetProgress()
	{
		if (!loggedIn)
		{
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(this,
			"Reset ALL Runeclicker progress for this character?\n"
			+ "Points, tiers, upgrades, prestiges and ascensions will be wiped.\n"
			+ "This cannot be undone. (Achievements are kept.)",
			"Reset progress?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm != JOptionPane.YES_OPTION)
		{
			return;
		}
		resetAll();
		stateOwnerKey = loadedProfileKey;
		scheduleNextGolden();
		saveState();
		refreshUi();
	}

	private void saveState()
	{
		// Never write one character's state onto another: if the active profile
		// changed since we loaded, skip the save and reload instead
		String currentKey = configManager.getRSProfileKey();
		if (!loggedIn || currentKey == null || !currentKey.equals(loadedProfileKey))
		{
			if (currentKey != null && !currentKey.equals(loadedProfileKey))
			{
				refresh();
			}
			return;
		}
		ticksSinceSave = 0;
		String state = STATE_VERSION + "|" + tier + "|" + points + "|" + ascensions
			+ "|" + pouchLevel + "|" + (daeyaltOwned ? "1" : "0") + "|" + joinCounts(upgradeCounts)
			+ "|" + lifetimePoints + "|" + totalClicks + "|" + totalPrestiges
			+ "|" + milestoneMask + "|" + joinCounts(perkCounts)
			+ "|" + goldenSeen + "|" + goldenCaught + "|" + critCount;
		configManager.setRSProfileConfiguration(SkedpojkarConfig.GROUP, STATE_KEY, state);
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

	// ---- UI ----

	private void showFeedback(String text, java.awt.Color color)
	{
		feedbackLabel.setText(text);
		feedbackLabel.setForeground(color != null ? color : pointsLabel.getForeground());
		clickFeedbackUntilMs = System.currentTimeMillis() + CLICK_FEEDBACK_MS;
	}

	private String upgradeDisplayName(int i)
	{
		// The talisman is flavored after the current rune tier
		return i == 0 ? RUNE_NAMES[tier] + " " + UPGRADE_NAMES[0] : UPGRADE_NAMES[i];
	}

	private void refreshUi()
	{
		if (!loggedIn)
		{
			runeButton.setState("Log in", false);
			pointsLabel.setText("Log in to craft runes.");
			rateLabel.setText(" ");
			prestigeButton.setText("Prestige");
			prestigeButton.setEnabled(false);
			for (JButton b : upgradeButtons)
			{
				b.setEnabled(false);
			}
			for (JButton b : perkButtons)
			{
				b.setEnabled(false);
			}
			pouchButton.setEnabled(false);
			daeyaltButton.setEnabled(false);
			statsLabel.setText(" ");
			return;
		}

		runeButton.setState(RUNE_NAMES[tier], true);
		setRuneIcon();
		pointsLabel.setText(format(points) + " points");
		rateLabel.setText("+" + format(pointsPerClick()) + "/click, +" + format(pointsPerSecondExpected()) + "/s");

		double gm = globalMultiplier();
		String gmExplained = "global = 2^tier x 3^attunement"
			+ " (x2 Daeyalt, x" + FRENZY_MULTIPLIER + " frenzy while active) = " + formatRate(gm);
		rateLabel.setToolTipText("<html>Click: (1 + talismans) x 2^pouches x global"
			+ " (" + (int) (CRIT_CHANCE * 100) + "% chance of x" + CRIT_MULTIPLIER + " crit)<br>"
			+ "Idle: sum of upgrades x global + auto-clicks<br>" + gmExplained + "</html>");

		for (int i = 0; i < UPGRADE_NAMES.length; i++)
		{
			// Show what buying one actually yields right now, ALL multipliers
			// included — talismans are boosted by pouches too, not just tier.
			// ZMI shows its expected yield to agree with the top rate line.
			double idleRate = UPGRADE_IDLE_RATES[i] * gm * (i == ZMI_INDEX ? zmiYieldFactor() : 1);
			String effect = i == 0
				? "+" + formatRate(Math.pow(2, pouchLevel) * gm) + "/click"
				: "+" + formatRate(idleRate) + "/s";
			upgradeLabels[i].setText(html(upgradeDisplayName(i) + " x" + upgradeCounts[i]
				+ " (" + effect + ")<br>Cost: " + format(upgradeCost(i))));
			upgradeButtons[i].setEnabled(points >= upgradeCost(i));

			if (i == 0)
			{
				upgradeLabels[i].setToolTipText("<html>Points/click = (1 + talismans) x 2^pouches x global<br>"
					+ "= (1 + " + upgradeCounts[0] + ") x " + (int) Math.pow(2, pouchLevel) + " x " + formatRate(gm)
					+ " = " + format(pointsPerClick()) + "<br>" + gmExplained + "</html>");
			}
			else if (i == ZMI_INDEX)
			{
				String zmiLine = perkCounts[PERK_PERFECT_ZMI] > 0
					? "Perfect Ourania: never fails."
					: "10% of seconds yield nothing (it's ZMI).";
				upgradeLabels[i].setToolTipText("<html>Each produces " + UPGRADE_IDLE_RATES[i]
					+ "/s base x global. " + zmiLine + "<br>Expected: "
					+ formatRate(idleRate) + "/s<br>" + gmExplained + "</html>");
			}
			else
			{
				upgradeLabels[i].setToolTipText("<html>Each produces " + UPGRADE_IDLE_RATES[i]
					+ "/s base x global = " + formatRate(idleRate) + "/s<br>" + gmExplained + "</html>");
			}
		}

		// The talisman icon follows the current tier
		if (tier != talismanIconTier)
		{
			talismanIconTier = tier;
			setItemIcon(upgradeIconLabels[0], TALISMAN_ICON_IDS[tier]);
		}

		// The pouch icon shows the next pouch to buy (or the last one when maxed)
		int pouchIconIdx = Math.min(pouchLevel, POUCH_ICON_IDS.length - 1);
		if (pouchIconIdx != pouchIconLevel)
		{
			pouchIconLevel = pouchIconIdx;
			setItemIcon(pouchIconLabel, POUCH_ICON_IDS[pouchIconIdx]);
		}

		if (pouchLevel >= POUCH_NAMES.length)
		{
			pouchLabel.setText(html("All pouches owned<br>(x" + (int) Math.pow(2, pouchLevel) + "/click)"));
			pouchButton.setEnabled(false);
		}
		else
		{
			pouchLabel.setText(html(POUCH_NAMES[pouchLevel] + " (x2/click)<br>Cost: "
				+ format(POUCH_COSTS[pouchLevel])));
			pouchButton.setEnabled(points >= POUCH_COSTS[pouchLevel]);
		}
		pouchLabel.setToolTipText("<html>Each pouch doubles points per click —<br>"
			+ "including what talismans add. Currently x" + (int) Math.pow(2, pouchLevel)
			+ " with " + pouchLevel + " pouch(es).<br>Does not affect idle income."
			+ (perkCounts[PERK_POUCH_KEEPER] > 0 ? "<br>Pouch keeper: kept on prestige." : "") + "</html>");

		long now = System.currentTimeMillis();
		if (!daeyaltOwned)
		{
			daeyaltLabel.setText(html("Daeyalt shard infusion<br>(x2 for " + DAEYALT_BOOST_SECONDS
				+ " s) Cost: " + format(DAEYALT_COST)));
			daeyaltButton.setText("Buy");
			daeyaltButton.setEnabled(points >= DAEYALT_COST);
		}
		else if (now < daeyaltBoostEndMs)
		{
			daeyaltLabel.setText(html("Daeyalt boost ACTIVE: " + (daeyaltBoostEndMs - now) / 1000 + " s"));
			daeyaltButton.setText("Active");
			daeyaltButton.setEnabled(false);
		}
		else if (now < daeyaltCooldownEndMs)
		{
			daeyaltLabel.setText(html("Daeyalt recharging: " + (daeyaltCooldownEndMs - now) / 1000 + " s"));
			daeyaltButton.setText("Wait");
			daeyaltButton.setEnabled(false);
		}
		else
		{
			daeyaltLabel.setText(html("Daeyalt shard infusion ready"));
			daeyaltButton.setText("Infuse");
			daeyaltButton.setEnabled(true);
		}
		daeyaltLabel.setToolTipText("<html>While active: ALL income x2 (clicks and idle)<br>for "
			+ DAEYALT_BOOST_SECONDS + " s, then " + (DAEYALT_COOLDOWN_SECONDS / 60) + " min cooldown.</html>");

		// Runespan perks: teased before the first ascension, usable after
		boolean showPerks = ascensions > 0;
		int unspent = unspentRunespanPoints();
		perkHeaderLabel.setVisible(true);
		if (showPerks)
		{
			perkHeaderLabel.setText(html("Runespan perks — " + unspent + " point(s)"));
			perkHeaderLabel.setToolTipText(null);
		}
		else
		{
			perkHeaderLabel.setText(html("??? — Ascend the Runespan to unlock"));
			perkHeaderLabel.setToolTipText("<html>Reach Wrath runes and Ascend to earn Runespan points,<br>"
				+ "spent here on permanent perks.</html>");
		}
		for (int i = 0; i < PERK_NAMES.length; i++)
		{
			perkRows[i].setVisible(showPerks);
			if (!showPerks)
			{
				continue;
			}
			boolean maxed = !PERK_REPEATABLE[i] && perkCounts[i] > 0;
			perkLabels[i].setText(html(PERK_NAMES[i] + " x" + perkCounts[i]
				+ (maxed ? " (owned)" : "<br>Cost: 1 Runespan point")));
			perkLabels[i].setToolTipText("<html>" + PERK_DESCRIPTIONS[i] + "</html>");
			perkButtons[i].setEnabled(unspent >= 1 && !maxed);
		}

		statsLabel.setText(html("Lifetime: " + format(lifetimePoints) + " points, "
			+ format(totalClicks) + " clicks<br>Prestiges: " + totalPrestiges
			+ ", Ascensions: " + ascensions
			+ "<br>Golden runes caught: " + goldenCaught + "/" + goldenSeen
			+ "<br>Crits landed: " + format(critCount)
			+ "<br><br>Tip: Shift-click a Buy button to buy up to " + BULK_BUY_COUNT + "."));

		refreshAchievements();

		// Two lines: action on top, full cost below — no hover needed to read it
		String prestigeLine = tier >= MAX_TIER
			? "Ascend the Runespan" : "Prestige: " + RUNE_NAMES[tier + 1];
		prestigeButton.setText("<html><center>" + prestigeLine
			+ "<br>" + format(prestigeCost()) + "</center></html>");
		prestigeButton.setToolTipText("Cost: " + format(prestigeCost()) + " points");
		prestigeButton.setEnabled(points >= prestigeCost());
	}

	private void refreshAchievements()
	{
		java.util.Set<String> unlocked = achievements.getUnlocked();
		Achievement[] all = Achievement.values();
		achievementsHeader.setText((achievementsExpanded ? "▼ " : "▶ ")
			+ "Achievements (" + unlocked.size() + "/" + all.length + ")");
		achievementsHeader.setToolTipText(achievementsExpanded
			? "Click to collapse" : "Click to expand");

		for (int i = 0; i < all.length; i++)
		{
			achievementLabels[i].setVisible(achievementsExpanded);
			if (!achievementsExpanded)
			{
				continue;
			}
			Achievement a = all[i];
			boolean got = unlocked.contains(a.name());
			if (got)
			{
				achievementLabels[i].setText(html("&#10003; " + a.getDisplayName()));
				achievementLabels[i].setForeground(ColorScheme.BRAND_ORANGE);
				achievementLabels[i].setToolTipText(a.getDescription() + " — done!");
			}
			else if (a.isSecret())
			{
				achievementLabels[i].setText(html("? ???"));
				achievementLabels[i].setForeground(java.awt.Color.GRAY);
				achievementLabels[i].setToolTipText("A secret achievement. Figure it out.");
			}
			else
			{
				achievementLabels[i].setText(html("&#9675; " + a.getDisplayName()));
				achievementLabels[i].setForeground(java.awt.Color.GRAY);
				achievementLabels[i].setToolTipText("How: " + a.getDescription());
			}
		}
	}

	private void setRuneIcon()
	{
		AsyncBufferedImage img = itemManager.getImage(RUNE_ITEM_IDS[tier]);
		Runnable apply = () -> runeButton.setSprite(img);
		apply.run();
		img.onLoaded(apply);
	}

	/**
	 * The clickable rune: a custom-painted sprite with a smoothly animated
	 * scale. Hover eases up to 110%, pressing dips to 90% (brightened),
	 * releasing springs back — always expanding/shrinking around the center.
	 */
	private class RuneButton extends javax.swing.JComponent
	{
		private java.awt.image.BufferedImage sprite;
		private java.awt.image.BufferedImage brightSprite;
		private String label = "";
		private boolean clickable;
		private boolean hovered;
		private boolean pressed;
		private double scale = 1.0;
		private double targetScale = 1.0;
		private final Timer anim = new Timer(16, e -> stepAnim());

		RuneButton()
		{
			setPreferredSize(new Dimension(120, 116));
			setMaximumSize(new Dimension(120, 116));
			setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
			addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseEntered(java.awt.event.MouseEvent e)
				{
					hovered = true;
					setTarget(1.1);
				}

				@Override
				public void mouseExited(java.awt.event.MouseEvent e)
				{
					hovered = false;
					pressed = false;
					setTarget(1.0);
				}

				@Override
				public void mousePressed(java.awt.event.MouseEvent e)
				{
					if (!clickable)
					{
						return;
					}
					pressed = true;
					setTarget(0.9);
				}

				@Override
				public void mouseReleased(java.awt.event.MouseEvent e)
				{
					boolean wasPressed = pressed;
					pressed = false;
					setTarget(hovered ? 1.1 : 1.0);
					if (wasPressed && clickable && contains(e.getPoint()))
					{
						onRuneClicked();
					}
				}
			});
		}

		void setSprite(java.awt.image.BufferedImage src)
		{
			// Copy into a plain ARGB buffer (drawn scaled at paint time) and
			// derive the brightened pressed-state version once
			java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(
				src.getWidth(), src.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = buf.createGraphics();
			g.drawImage(src, 0, 0, null);
			g.dispose();
			sprite = buf;
			brightSprite = new java.awt.image.RescaleOp(
				new float[]{1.25f, 1.25f, 1.25f, 1f}, new float[4], null).filter(buf, null);
			repaint();
		}

		void setState(String newLabel, boolean nowClickable)
		{
			label = newLabel;
			clickable = nowClickable;
			repaint();
		}

		private void setTarget(double target)
		{
			targetScale = target;
			if (!anim.isRunning())
			{
				anim.start();
			}
		}

		private void stepAnim()
		{
			scale += (targetScale - scale) * 0.3; // ease toward target
			if (Math.abs(scale - targetScale) < 0.004)
			{
				scale = targetScale;
				anim.stop();
			}
			repaint();
		}

		@Override
		protected void paintComponent(java.awt.Graphics g)
		{
			super.paintComponent(g);
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
				java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			int spriteArea = getHeight() - 20; // bottom strip reserved for the label
			java.awt.image.BufferedImage img = pressed && brightSprite != null ? brightSprite : sprite;
			if (img != null && clickable)
			{
				int size = (int) (RUNE_SIZE * scale);
				g2.drawImage(img, (getWidth() - size) / 2, (spriteArea - size) / 2, size, size, null);
			}

			g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
			g2.setColor(getForeground());
			java.awt.FontMetrics fm = g2.getFontMetrics();
			g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, getHeight() - 5);
			g2.dispose();
		}
	}

	/**
	 * Hosts the rune button and paints crit fire over it: flame-colored embers
	 * (plus the occasional tiny fire-rune sprite) rise from the rune, drift,
	 * and fade out over ~a second.
	 */
	private class ConfettiPane extends JPanel
	{
		// x, y, xVelocity, yVelocity, colorIndex, life (1.0 -> 0), isIcon (0/1)
		private final java.util.List<double[]> particles = new java.util.ArrayList<>();
		private final java.awt.Color[] colors = {
			new java.awt.Color(255, 69, 0), new java.awt.Color(255, 140, 0),
			new java.awt.Color(255, 200, 60), new java.awt.Color(255, 90, 40),
			new java.awt.Color(220, 50, 30),
		};
		private final Timer animator = new Timer(30, e -> step());
		private java.awt.Image fireIcon;
		private boolean fireIconRequested;

		ConfettiPane()
		{
			super(new java.awt.GridBagLayout());
			setOpaque(false);
			Dimension size = new Dimension(180, 140);
			setPreferredSize(size);
			setMaximumSize(size);
			// NOTE: no itemManager use here — this constructor runs as a field
			// initializer, before the outer panel's constructor assigns it
		}

		void burst()
		{
			// Lazy-load the fire-rune sprite on first use (itemManager is set by now)
			if (!fireIconRequested)
			{
				fireIconRequested = true;
				AsyncBufferedImage img = itemManager.getImage(ItemID.FIRE_RUNE);
				Runnable apply = () ->
					fireIcon = img.getScaledInstance(14, 14, java.awt.Image.SCALE_SMOOTH);
				apply.run();
				img.onLoaded(apply);
			}
			double cx = getWidth() / 2.0;
			double cy = getHeight() / 2.0;
			for (int i = 0; i < 24; i++)
			{
				boolean icon = random.nextInt(5) == 0; // ~20% are fire-rune sprites
				particles.add(new double[]{
					cx + (random.nextDouble() - 0.5) * 60, // spread across the rune
					cy + 10 + random.nextDouble() * 15,    // start near its base
					(random.nextDouble() - 0.5) * 1.6,     // slight sideways drift
					-(1.5 + random.nextDouble() * 2.5),    // rising
					random.nextInt(colors.length),
					0.8 + random.nextDouble() * 0.4,
					icon ? 1 : 0,
				});
			}
			if (!animator.isRunning())
			{
				animator.start();
			}
		}

		private void step()
		{
			for (java.util.Iterator<double[]> it = particles.iterator(); it.hasNext(); )
			{
				double[] p = it.next();
				p[0] += p[2] + Math.sin(p[1] * 0.15) * 0.6; // flicker/wobble as it rises
				p[1] += p[3];
				p[3] -= 0.05; // buoyancy: embers accelerate upward slightly
				p[5] -= 0.04;
				if (p[5] <= 0)
				{
					it.remove();
				}
			}
			if (particles.isEmpty())
			{
				animator.stop();
			}
			repaint();
		}

		@Override
		public void paint(java.awt.Graphics g)
		{
			super.paint(g); // children (the rune button) first, fire on top
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			for (double[] p : particles)
			{
				g2.setComposite(java.awt.AlphaComposite.getInstance(
					java.awt.AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, p[5]))));
				if (p[6] == 1 && fireIcon != null)
				{
					g2.drawImage(fireIcon, (int) p[0], (int) p[1], null);
				}
				else
				{
					g2.setColor(colors[(int) p[4]]);
					int size = 2 + (int) (p[5] * 4);
					g2.fillOval((int) p[0], (int) p[1], size, size);
				}
			}
			g2.dispose();
		}
	}

	/**
	 * HTML labels don't wrap to the space they're given — they demand the width
	 * of their longest line, which pushed the Buy buttons out of the narrow
	 * sidebar. A fixed body width forces wrapping instead.
	 */
	private static String html(String inner)
	{
		// Narrower than before: shop rows now also fit a 22px item icon
		return "<html><body style='width:76px'>" + inner + "</body></html>";
	}

	/** For per-second/per-click rates, which can be fractional (e.g. 0.5/s). */
	private static String formatRate(double value)
	{
		if (value >= 1_000)
		{
			return format(value);
		}
		return value == Math.floor(value)
			? String.valueOf((long) value)
			: String.format("%.1f", value);
	}

	private static String format(double value)
	{
		// Full digits with thousand separators ("1,000,000"), no suffixes.
		// Fallback to scientific notation only where long overflows (~9.2e18).
		if (value < Long.MAX_VALUE)
		{
			return String.format("%,d", (long) value);
		}
		return String.format("%.2e", value);
	}
}
