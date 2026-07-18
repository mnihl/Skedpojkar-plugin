package com.skedpojkar.panel;

import com.skedpojkar.SkedpojkarConfig;
import com.skedpojkar.multiplayer.PartyTicTacToe;
import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
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

	public SkedpojkarPanel(SkedpojkarConfig config, ConfigManager configManager, Client client,
		ClientThread clientThread, ItemManager itemManager, PartyTicTacToe partyTicTacToe)
	{
		super(false);
		setLayout(new BorderLayout());

		runeClickerPanel = new RuneClickerPanel(config, configManager, client, clientThread, itemManager);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Runeclicker", runeClickerPanel);
		tabs.addTab("TTT", new TicTacToePanel());
		tabs.addTab("Facts", new FactsPanel());
		tabs.addTab("Party", new MultiplayerPanel(partyTicTacToe));

		add(tabs, BorderLayout.CENTER);
	}
}
