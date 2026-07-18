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

	/**
	 * Pin the panel to RuneLite's standard sidebar width. Without this, wide
	 * content (the tab strip, long labels) increases our preferred width and
	 * the client resizes the whole game window to fit the sidebar.
	 */
	@Override
	public java.awt.Dimension getPreferredSize()
	{
		return new java.awt.Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
	}

	@Override
	public java.awt.Dimension getMinimumSize()
	{
		// Swing layouts fall back to minimum size when space is tight — if the
		// content's minimum exceeds the sidebar width, the client still grows
		return new java.awt.Dimension(PluginPanel.PANEL_WIDTH, super.getMinimumSize().height);
	}

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
