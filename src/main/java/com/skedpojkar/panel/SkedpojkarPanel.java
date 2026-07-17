package com.skedpojkar.panel;

import com.skedpojkar.multiplayer.PartyTicTacToe;
import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar panel: one tab per minigame/feature.
 */
public class SkedpojkarPanel extends PluginPanel
{
	@Getter
	private final RuneClickerPanel runeClickerPanel;

	public SkedpojkarPanel(ConfigManager configManager, ItemManager itemManager, PartyTicTacToe partyTicTacToe)
	{
		super(false);
		setLayout(new BorderLayout());

		runeClickerPanel = new RuneClickerPanel(configManager, itemManager);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Runes", runeClickerPanel);
		tabs.addTab("TTT", new TicTacToePanel());
		tabs.addTab("Facts", new FactsPanel());
		tabs.addTab("Party", new MultiplayerPanel(partyTicTacToe));

		add(tabs, BorderLayout.CENTER);
	}
}
