package com.skedpojkar.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.util.LinkBrowser;

/**
 * The Info tab: what the plugin does and how to use it, for people who will
 * never read the README. First tab so new users land on it.
 */
public class WelcomePanel extends JPanel
{
	private static final String ISSUES_URL = "https://github.com/mnihl/Skedpojkar-plugin/issues";

	public WelcomePanel()
	{
		setLayout(new BorderLayout());

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		content.setOpaque(false);

		header(content, "Welcome to Skedpojkar");
		text(content, "Fun sounds, chat messages and side-panel minigames.");

		header(content, "Announcements");
		text(content, "Sounds and/or chat messages for in-game events: your level-ups"
			+ " and deaths, PvP hits and kills, clan broadcasts (kills, deaths, drops),"
			+ " Hallowed Sepulchre floor 5, the Corrupted Gauntlet, passing the"
			+ " Al Kharid gate, and more. Every trigger has its own toggle in the"
			+ " plugin settings.");
		text(content, "Sounds work out of the box. To replace one, drop an uncompressed"
			+ " PCM .wav with the right name into .runelite/skedpojkar-sounds/ — the"
			+ " folder and a README listing the names are created automatically.");

		header(content, "Runeclicker");
		text(content, "A runecrafting idle clicker. Click the rune to craft, buy"
			+ " upgrades that craft for you, and prestige up the rune tiers"
			+ " Air → Wrath — each tier doubles ALL income, so rebuilding is"
			+ " fast. Beyond Wrath lies the Runespan.");
		text(content, "Crits, golden runes and milestones await. Shift-click a Buy"
			+ " button to buy up to 10 at once. Progress is saved per character and"
			+ " only accrues while you're logged in — no offline gains.");

		header(content, "Tic-tac-toe");
		text(content, "TTT: play against a simple AI. Party: play a friend for real —"
			+ " you both install this plugin and join the same party (the built-in"
			+ " Party plugin, same passphrase). Lowest party id is X and moves first.");

		header(content, "Bugs & ideas");
		text(content, "Something broken, or something you wish this plugin did?"
			+ " Tell us on GitHub:");
		JButton issues = new JButton("Open GitHub issues");
		issues.setAlignmentX(Component.LEFT_ALIGNMENT);
		issues.addActionListener(e -> LinkBrowser.browse(ISSUES_URL));
		content.add(issues);
		content.add(Box.createVerticalStrut(8));

		header(content, "Fair play");
		text(content, "This is a fan-made plugin for fun. It performs no input"
			+ " automation, sends nothing to the game, and never plays on your"
			+ " behalf.");

		JScrollPane scroll = new JScrollPane(content,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		add(scroll, BorderLayout.CENTER);
	}

	private static void header(JPanel parent, String title)
	{
		JLabel label = new JLabel(title);
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));
		parent.add(label);
	}

	private static void text(JPanel parent, String body)
	{
		JLabel label = new JLabel("<html><body style='width:165px'>" + body + "</body></html>");
		label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		parent.add(label);
	}
}
