package com.corgifeatures.panel;

import com.corgifeatures.multiplayer.PartyTicTacToe;
import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar panel: one tab per minigame/feature.
 */
public class CorgiFeaturesPanel extends PluginPanel
{
	public CorgiFeaturesPanel(ConfigManager configManager, PartyTicTacToe partyTicTacToe)
	{
		super(false);
		setLayout(new BorderLayout());

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Cookies", new CookieClickerPanel(configManager));
		tabs.addTab("TTT", new TicTacToePanel());
		tabs.addTab("Facts", new FactsPanel());
		tabs.addTab("Party", new MultiplayerPanel(partyTicTacToe));

		add(tabs, BorderLayout.CENTER);
	}
}
