package com.skedpojkar;

import com.skedpojkar.announce.AnnouncementTriggers;
import com.skedpojkar.multiplayer.PartyGameMessage;
import com.skedpojkar.multiplayer.PartyTicTacToe;
import com.skedpojkar.panel.SkedpojkarPanel;
import com.skedpojkar.sound.SoundEngine;
import com.google.inject.Provides;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Skedpojkar",
	description = "Sounds and chat announcements for events and specific players, plus side-panel minigames and party tic-tac-toe",
	tags = {"corgi", "sound", "announce", "minigame", "party", "fun"}
)
public class SkedpojkarPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private WSClient wsClient;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private AnnouncementTriggers announcementTriggers;

	@Inject
	private SoundEngine soundEngine;

	@Inject
	private PartyTicTacToe partyTicTacToe;

	private SkedpojkarPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		eventBus.register(announcementTriggers);
		eventBus.register(partyTicTacToe);
		wsClient.registerMessage(PartyGameMessage.class);
		executor.execute(soundEngine::init);

		panel = new SkedpojkarPanel(configManager, partyTicTacToe);
		navButton = NavigationButton.builder()
			.tooltip("Skedpojkar")
			.icon(ImageUtil.loadImageResource(SkedpojkarPlugin.class, "icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		log.info("Skedpojkar started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		wsClient.unregisterMessage(PartyGameMessage.class);
		eventBus.unregister(announcementTriggers);
		eventBus.unregister(partyTicTacToe);
		partyTicTacToe.setBoardListener(null);
		panel = null;
		navButton = null;

		log.info("Skedpojkar stopped");
	}

	/** The cookie count is stored per character, so re-read it when the character changes. */
	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		SkedpojkarPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(currentPanel.getCookieClickerPanel()::refresh);
		}
	}

	@Provides
	SkedpojkarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkedpojkarConfig.class);
	}
}
