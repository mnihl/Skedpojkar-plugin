package com.corgifeatures.panel;

import com.corgifeatures.CorgiFeaturesConfig;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.config.ConfigManager;

/**
 * Minimal cookie clicker. The count persists between sessions via ConfigManager.
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
			configManager.setConfiguration(CorgiFeaturesConfig.GROUP, COUNT_KEY, cookies);
		});

		JLabel hint = new JLabel("<html><center>Upgrades, corgis-per-second<br>and prestige: coming soon.</center></html>", SwingConstants.CENTER);

		add(countLabel, BorderLayout.NORTH);
		add(bakeButton, BorderLayout.CENTER);
		add(hint, BorderLayout.SOUTH);
	}

	private long loadCount()
	{
		try
		{
			String stored = configManager.getConfiguration(CorgiFeaturesConfig.GROUP, COUNT_KEY);
			return stored == null ? 0 : Long.parseLong(stored);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private void updateLabel()
	{
		countLabel.setText("Cookies: " + cookies);
	}
}
