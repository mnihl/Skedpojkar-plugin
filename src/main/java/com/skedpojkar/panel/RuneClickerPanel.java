package com.skedpojkar.panel;

import com.skedpojkar.SkedpojkarConfig;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Random;
import javax.swing.BorderFactory;
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
import net.runelite.api.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Runecrafting clicker (replaces the old cookie clicker; existing cookie
 * counts migrate into starting points once, per character).
 *
 * Click the rune to craft; buy upgrades that click/craft for you; prestige
 * through the real rune tiers (Air → Wrath) doubling your base points per
 * click each time; after Wrath, Ascend for a permanent global multiplier.
 * Idle income only accrues while the client is open and you're logged in.
 * All numbers are first-pass balance — tune freely (see RUNECLICKER_DESIGN.md).
 */
@Slf4j
public class RuneClickerPanel extends JPanel
{
	private static final String STATE_KEY = "runeClicker";
	private static final String LEGACY_COOKIE_KEY = "cookieCount";
	private static final int STATE_VERSION = 1;

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
	// points per click; the rest generate points per second.
	private static final String[] UPGRADE_NAMES = {
		"Chisel-sharpened talisman", "Rune essence miner", "Pure essence miner",
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

	private static final int AUTOSAVE_EVERY_TICKS = 10;

	private final ConfigManager configManager;
	private final ItemManager itemManager;
	private final Random random = new Random();

	// Game state (persisted per character)
	private int tier;
	private double points;
	private int ascensions;
	private final int[] upgradeCounts = new int[UPGRADE_NAMES.length];
	private int pouchLevel;
	private boolean daeyaltOwned;

	// Transient state
	private boolean loggedIn;
	private long daeyaltBoostEndMs;
	private long daeyaltCooldownEndMs;
	private int ticksSinceSave;

	// UI
	private final JButton runeButton = new JButton();
	private final JLabel pointsLabel = new JLabel("", SwingConstants.CENTER);
	private final JLabel rateLabel = new JLabel("", SwingConstants.CENTER);
	private final JButton[] upgradeButtons = new JButton[UPGRADE_NAMES.length];
	private final JLabel[] upgradeLabels = new JLabel[UPGRADE_NAMES.length];
	private final JButton pouchButton = new JButton();
	private final JLabel pouchLabel = new JLabel();
	private final JButton daeyaltButton = new JButton();
	private final JLabel daeyaltLabel = new JLabel();
	private final JButton prestigeButton = new JButton();

	public RuneClickerPanel(ConfigManager configManager, ItemManager itemManager)
	{
		this.configManager = configManager;
		this.itemManager = itemManager;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Top: the rune to click + counters. Square button, icon above the name.
		runeButton.setFocusPainted(false);
		runeButton.setPreferredSize(new Dimension(84, 84));
		runeButton.setMaximumSize(new Dimension(84, 84));
		runeButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		runeButton.setHorizontalTextPosition(SwingConstants.CENTER);
		runeButton.addActionListener(e -> onRuneClicked());
		pointsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		rateLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);
		runeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		pointsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		rateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		prestigeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		top.add(runeButton);
		top.add(pointsLabel);
		top.add(rateLabel);
		top.add(javax.swing.Box.createVerticalStrut(6));
		top.add(prestigeButton);
		top.add(javax.swing.Box.createVerticalStrut(4));

		// Middle: the shop
		JPanel shop = new JPanel();
		shop.setLayout(new BoxLayout(shop, BoxLayout.Y_AXIS));
		shop.setOpaque(false);
		for (int i = 0; i < UPGRADE_NAMES.length; i++)
		{
			final int idx = i;
			upgradeButtons[i] = new JButton("Buy");
			upgradeButtons[i].addActionListener(e -> buyUpgrade(idx));
			upgradeLabels[i] = new JLabel();
			shop.add(shopRow(upgradeLabels[i], upgradeButtons[i]));
		}
		pouchButton.setText("Buy");
		pouchButton.addActionListener(e -> buyPouch());
		shop.add(shopRow(pouchLabel, pouchButton));
		daeyaltButton.addActionListener(e -> onDaeyaltButton());
		shop.add(shopRow(daeyaltLabel, daeyaltButton));

		JScrollPane scroll = new JScrollPane(shop,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());

		// Bottom: prestige / ascend
		prestigeButton.addActionListener(e -> onPrestige());

		add(top, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		refresh();

		// 1 s game tick: idle income, boost timers, UI refresh, throttled saves.
		// Swing timers only fire while the client runs = no offline progress.
		Timer timer = new Timer(1000, e -> tick());
		timer.start();
	}

	private JPanel shopRow(JLabel label, JButton button)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
		label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		button.setMargin(new Insets(2, 6, 2, 6));
		row.add(label, BorderLayout.CENTER);
		row.add(button, BorderLayout.EAST);
		return row;
	}

	/**
	 * HTML labels don't wrap to the space they're given — they demand the width
	 * of their longest line, which pushed the Buy buttons out of the narrow
	 * sidebar. A fixed body width forces wrapping instead.
	 */
	private static String html(String inner)
	{
		return "<html><body style='width:105px'>" + inner + "</body></html>";
	}

	/** Reloads state for the current character. Called on login/account switch. */
	public void refresh()
	{
		loggedIn = configManager.getRSProfileKey() != null;
		if (loggedIn)
		{
			loadState();
		}
		refreshUi();
	}

	// ---- Game logic ----

	/**
	 * Applies to ALL income, clicks and idle alike: x2 per rune tier (so each
	 * prestige doubles your whole economy and rebuilding is fast — the classic
	 * clicker prestige loop), x10 per ascension, x2 while the Daeyalt boost runs.
	 */
	private double globalMultiplier()
	{
		double mult = Math.pow(2, tier) * Math.pow(10, ascensions);
		if (System.currentTimeMillis() < daeyaltBoostEndMs)
		{
			mult *= 2;
		}
		return mult;
	}

	private double pointsPerClick()
	{
		double base = 1 + upgradeCounts[0];
		return base * Math.pow(2, pouchLevel) * globalMultiplier();
	}

	/** Expected idle income per second (ZMI counted at its average yield). */
	private double pointsPerSecondExpected()
	{
		double sum = 0;
		for (int i = 1; i < UPGRADE_NAMES.length; i++)
		{
			double rate = UPGRADE_IDLE_RATES[i] * upgradeCounts[i];
			if (i == ZMI_INDEX)
			{
				rate *= 1 - ZMI_FAIL_CHANCE;
			}
			sum += rate;
		}
		return sum * globalMultiplier();
	}

	private double upgradeCost(int idx)
	{
		return UPGRADE_BASE_COSTS[idx] * Math.pow(1.15, upgradeCounts[idx]);
	}

	private double prestigeCost()
	{
		return 10_000 * Math.pow(10, tier);
	}

	private void onRuneClicked()
	{
		if (!loggedIn)
		{
			return;
		}
		points += pointsPerClick();
		refreshUi();
	}

	private void buyUpgrade(int idx)
	{
		double cost = upgradeCost(idx);
		if (!loggedIn || points < cost)
		{
			return;
		}
		points -= cost;
		upgradeCounts[idx]++;
		saveState();
		refreshUi();
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
		String warning = ascend
			? "Ascend the Runespan? EVERYTHING resets (back to Air runes), but you gain a permanent x10 income multiplier."
			: "Prestige to " + RUNE_NAMES[tier + 1] + " runes? Points and upgrades reset; your base points per click doubles permanently.";
		if (JOptionPane.showConfirmDialog(this, warning, "Are you sure?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
		{
			return;
		}

		points = 0;
		java.util.Arrays.fill(upgradeCounts, 0);
		pouchLevel = 0;
		daeyaltOwned = false;
		daeyaltBoostEndMs = 0;
		daeyaltCooldownEndMs = 0;
		if (ascend)
		{
			tier = 0;
			ascensions++;
		}
		else
		{
			tier++;
		}
		saveState();
		refreshUi();
	}

	private void tick()
	{
		// Re-check login state every tick: logging out must stop the clock
		// (no offline/logged-out progress), and logging in must load the
		// character's state before any income accrues.
		boolean nowLoggedIn = configManager.getRSProfileKey() != null;
		if (!nowLoggedIn)
		{
			if (loggedIn)
			{
				loggedIn = false;
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
			if (i == ZMI_INDEX && random.nextDouble() < ZMI_FAIL_CHANCE)
			{
				rate = 0; // it's random, it's ZMI
			}
			income += rate;
		}
		points += income * globalMultiplier();

		if (++ticksSinceSave >= AUTOSAVE_EVERY_TICKS)
		{
			saveState();
		}
		refreshUi();
	}

	// ---- Persistence ----

	private void loadState()
	{
		String stored = configManager.getRSProfileConfiguration(SkedpojkarConfig.GROUP, STATE_KEY);
		if (stored == null)
		{
			migrateFromCookies();
			return;
		}

		try
		{
			String[] parts = stored.split("\\|");
			// parts[0] is the version, unused until the format changes
			tier = Integer.parseInt(parts[1]);
			points = Double.parseDouble(parts[2]);
			ascensions = Integer.parseInt(parts[3]);
			pouchLevel = Integer.parseInt(parts[4]);
			daeyaltOwned = "1".equals(parts[5]);
			String[] counts = parts[6].split(",");
			for (int i = 0; i < upgradeCounts.length; i++)
			{
				upgradeCounts[i] = i < counts.length ? Integer.parseInt(counts[i]) : 0;
			}
		}
		catch (Exception e)
		{
			log.warn("Corrupt rune clicker state, starting fresh: {}", stored, e);
			resetAll();
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
		saveState();
	}

	private void resetAll()
	{
		tier = 0;
		points = 0;
		ascensions = 0;
		pouchLevel = 0;
		daeyaltOwned = false;
		java.util.Arrays.fill(upgradeCounts, 0);
	}

	private void saveState()
	{
		if (!loggedIn)
		{
			return;
		}
		ticksSinceSave = 0;
		StringBuilder counts = new StringBuilder();
		for (int i = 0; i < upgradeCounts.length; i++)
		{
			if (i > 0)
			{
				counts.append(',');
			}
			counts.append(upgradeCounts[i]);
		}
		String state = STATE_VERSION + "|" + tier + "|" + points + "|" + ascensions
			+ "|" + pouchLevel + "|" + (daeyaltOwned ? "1" : "0") + "|" + counts;
		configManager.setRSProfileConfiguration(SkedpojkarConfig.GROUP, STATE_KEY, state);
	}

	// ---- UI ----

	private void refreshUi()
	{
		if (!loggedIn)
		{
			runeButton.setText("Log in");
			runeButton.setIcon(null);
			pointsLabel.setText("Log in to craft runes.");
			rateLabel.setText(" ");
			prestigeButton.setText("Prestige");
			prestigeButton.setEnabled(false);
			for (JButton b : upgradeButtons)
			{
				b.setEnabled(false);
			}
			pouchButton.setEnabled(false);
			daeyaltButton.setEnabled(false);
			return;
		}

		runeButton.setText(RUNE_NAMES[tier]);
		setRuneIcon();
		String asc = ascensions > 0 ? ("  (Ascension " + ascensions + ")") : "";
		pointsLabel.setText(format(points) + " points" + asc);
		rateLabel.setText("+" + format(pointsPerClick()) + "/click, +" + format(pointsPerSecondExpected()) + "/s");

		double gm = globalMultiplier();
		String gmExplained = "global multiplier = 2^tier x 10^ascensions"
			+ " (x2 during Daeyalt boost) = " + formatRate(gm);

		for (int i = 0; i < UPGRADE_NAMES.length; i++)
		{
			// Show what buying one actually yields right now, ALL multipliers
			// included — talismans are boosted by pouches too, not just tier.
			// ZMI shows its expected yield (90%) to agree with the top rate line.
			double idleRate = UPGRADE_IDLE_RATES[i] * gm
				* (i == ZMI_INDEX ? 1 - ZMI_FAIL_CHANCE : 1);
			String effect = i == 0
				? "+" + formatRate(Math.pow(2, pouchLevel) * gm) + "/click"
				: "+" + formatRate(idleRate) + "/s";
			upgradeLabels[i].setText(html(UPGRADE_NAMES[i] + " x" + upgradeCounts[i]
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
				upgradeLabels[i].setToolTipText("<html>Each produces " + UPGRADE_IDLE_RATES[i]
					+ "/s base x global, but 10% of seconds yield nothing (it's ZMI).<br>"
					+ "Expected: " + UPGRADE_IDLE_RATES[i] + " x 0.9 x " + formatRate(gm)
					+ " = " + formatRate(idleRate) + "/s<br>" + gmExplained + "</html>");
			}
			else
			{
				upgradeLabels[i].setToolTipText("<html>Each produces " + UPGRADE_IDLE_RATES[i]
					+ "/s base x global = " + formatRate(idleRate) + "/s<br>"
					+ gmExplained + "</html>");
			}
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
			+ " with " + pouchLevel + " pouch(es).<br>Does not affect idle income.</html>");
		daeyaltButton.setToolTipText(null);
		daeyaltLabel.setToolTipText("<html>While active: ALL income x2 (clicks and idle)<br>for "
			+ DAEYALT_BOOST_SECONDS + " s, then " + (DAEYALT_COOLDOWN_SECONDS / 60) + " min cooldown.</html>");
		rateLabel.setToolTipText("<html>Click: (1 + talismans) x 2^pouches x global<br>"
			+ "Idle: sum of upgrades x global<br>" + gmExplained + "</html>");

		long now = System.currentTimeMillis();
		if (!daeyaltOwned)
		{
			daeyaltLabel.setText(html("Daeyalt shard infusion<br>(x2 for 60 s) Cost: " + format(DAEYALT_COST)));
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

		if (tier >= MAX_TIER)
		{
			prestigeButton.setText("Ascend the Runespan (" + format(prestigeCost()) + ")");
		}
		else
		{
			prestigeButton.setText("Prestige: " + RUNE_NAMES[tier + 1] + " (" + format(prestigeCost()) + ")");
		}
		prestigeButton.setEnabled(points >= prestigeCost());
	}

	private void setRuneIcon()
	{
		AsyncBufferedImage img = itemManager.getImage(RUNE_ITEM_IDS[tier]);
		runeButton.setIcon(new ImageIcon(img));
		img.onLoaded(() ->
		{
			runeButton.setIcon(new ImageIcon(img));
			runeButton.repaint();
		});
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
		// Full digits with separators below a million ("25,000") — easier to
		// read at small font sizes than "25.00K"
		if (value < 1_000_000)
		{
			return String.format("%,d", (long) value);
		}
		String[] suffixes = {"M", "B", "T", "Qa", "Qi"};
		double v = value / 1_000_000;
		int i = 0;
		while (v >= 1_000 && i < suffixes.length - 1)
		{
			v /= 1_000;
			i++;
		}
		return String.format("%.1f%s", v, suffixes[i]);
	}
}
