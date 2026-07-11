package com.skedpojkar.panel;

import com.skedpojkar.SkedpojkarConfig;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.config.ConfigManager;

/**
 * Minimal cookie clicker. The count persists per character (RS profile); counts
 * baked while logged out, or from before per-character storage existed, live
 * under a shared account-wide key that doubles as a migration fallback.
 */
public class CookieClickerPanel extends JPanel
{
	private static final String COUNT_KEY = "cookieCount";

	private final ConfigManager configManager;
	private final JLabel countLabel = new JLabel("", SwingConstants.CENTER);

	private long cookies;

	public CookieClickerPanel(ConfigManager configManager)
	{
		this.configManager = configManager;
		this.cookies = loadCount();

		setLayout(new BorderLayout(0, 10));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 16f));
		updateLabel();

		JButton bakeButton = new JButton("Bake a cookie");
		bakeButton.setFont(bakeButton.getFont().deriveFont(Font.BOLD, 14f));
		bakeButton.addActionListener(e ->
		{
			cookies++;
			updateLabel();
			saveCount();
		});

		JLabel hint = new JLabel("<html><center>Upgrades, corgis-per-second<br>and prestige: coming soon.</center></html>", SwingConstants.CENTER);

		add(countLabel, BorderLayout.NORTH);
		add(bakeButton, BorderLayout.CENTER);
		add(hint, BorderLayout.SOUTH);
	}

	/** Re-reads the count for the current character. Called on login/account switch. */
	public void refresh()
	{
		cookies = loadCount();
		updateLabel();
	}

	private long loadCount()
	{
		String stored = configManager.getRSProfileConfiguration(SkedpojkarConfig.GROUP, COUNT_KEY);
		if (stored == null)
		{
			// Not logged in yet, or this character has no count: fall back to the shared key
			stored = configManager.getConfiguration(SkedpojkarConfig.GROUP, COUNT_KEY);
		}
		try
		{
			return stored == null ? 0 : Long.parseLong(stored);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private void saveCount()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(SkedpojkarConfig.GROUP, COUNT_KEY, cookies);
		}
		else
		{
			// Logged out: keep the count in the shared key so it isn't lost
			configManager.setConfiguration(SkedpojkarConfig.GROUP, COUNT_KEY, cookies);
		}
	}

	private void updateLabel()
	{
		countLabel.setText("Cookies: " + cookies);
	}
}
