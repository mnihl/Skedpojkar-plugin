package com.corgifeatures.panel;

import com.corgifeatures.multiplayer.PartyTicTacToe;
import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar panel: one tab per minigame/feature.
 */
public class CorgiFeaturesPanel extends PluginPanel
{
	@Getter
	private final CookieClickerPanel cookieClickerPanel;

	public CorgiFeaturesPanel(ConfigManager configManager, PartyTicTacToe partyTicTacToe)
	{
		super(false);
		setLayout(new BorderLayout());

		cookieClickerPanel = new CookieClickerPanel(configManager);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Cookies", cookieClickerPanel);
		tabs.addTab("TTT", new TicTacToePanel());
		tabs.addTab("Facts", new FactsPanel());
		tabs.addTab("Party", new MultiplayerPanel(partyTicTacToe));

		add(tabs, BorderLayout.CENTER);
	}
}
