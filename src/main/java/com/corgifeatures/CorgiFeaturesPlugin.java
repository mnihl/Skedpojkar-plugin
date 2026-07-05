package com.corgifeatures;

import com.corgifeatures.announce.AnnouncementTriggers;
import com.corgifeatures.multiplayer.PartyGameMessage;
import com.corgifeatures.multiplayer.PartyTicTacToe;
import com.corgifeatures.panel.CorgiFeaturesPanel;
import com.corgifeatures.sound.SoundEngine;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Corgi's Various Features",
	description = "Sounds and chat announcements for events and specific players, plus side-panel minigames and party tic-tac-toe",
	tags = {"corgi", "sound", "announce", "minigame", "party", "fun"}
)
public class CorgiFeaturesPlugin extends Plugin
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

	private CorgiFeaturesPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		eventBus.register(announcementTriggers);
		eventBus.register(partyTicTacToe);
		wsClient.registerMessage(PartyGameMessage.class);
		executor.execute(soundEngine::init);

		panel = new CorgiFeaturesPanel(configManager, partyTicTacToe);
		navButton = NavigationButton.builder()
			.tooltip("Corgi's Various Features")
			.icon(createIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		log.info("Corgi's Various Features started");
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

		log.info("Corgi's Various Features stopped");
	}

	@Provides
	CorgiFeaturesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CorgiFeaturesConfig.class);
	}

	// Drawn programmatically until the plugin has a real icon resource
	private BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(222, 143, 62));
		g.fillRoundRect(0, 0, 16, 16, 6, 6);
		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		g.drawString("C", 4, 13);
		g.dispose();
		return image;
	}
}
