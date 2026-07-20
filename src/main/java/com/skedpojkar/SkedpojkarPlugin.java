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
	description = "Side panel idle-minigames, toggleable fun sounds and chat messages during certain in-game events.",
	tags = {"sound", "announce", "minigame", "party", "clan", "fun"}
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
	private net.runelite.client.game.ItemManager itemManager;

	@Inject
	private net.runelite.api.Client client;

	@Inject
	private net.runelite.client.callback.ClientThread clientThread;

	@Inject
	private SkedpojkarConfig config;

	@Inject
	private com.skedpojkar.achievements.AchievementManager achievementManager;

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

		panel = new SkedpojkarPanel(config, configManager, client, clientThread, itemManager, partyTicTacToe, achievementManager);
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
		// navButton can be null if startUp failed partway through
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		wsClient.unregisterMessage(PartyGameMessage.class);
		eventBus.unregister(announcementTriggers);
		eventBus.unregister(partyTicTacToe);
		partyTicTacToe.setBoardListener(null);
		panel = null;
		navButton = null;

		log.info("Skedpojkar stopped");
	}

	/** Clicker progress is stored per character, so re-read it when the character changes. */
	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		SkedpojkarPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(currentPanel.getRuneClickerPanel()::refresh);
		}
	}

	@Provides
	SkedpojkarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkedpojkarConfig.class);
	}
}
