package com.corgifeatures.announce;

import com.corgifeatures.CorgiFeaturesConfig;
import com.corgifeatures.sound.Sound;
import com.corgifeatures.sound.SoundEngine;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Feature 1: reacts to in-game events with local chat messages and sounds,
 * optionally scoped to specific usernames from config.
 */
@Slf4j
@Singleton
public class AnnouncementTriggers
{
	// A target walking in and out of render distance would otherwise re-trigger constantly
	private static final long SPAWN_COOLDOWN_MS = 10_000L;

	@Inject
	private Client client;

	@Inject
	private CorgiFeaturesConfig config;

	@Inject
	private SoundEngine soundEngine;

	private final Map<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private final Map<String, Long> lastSpawnAnnounce = new HashMap<>();

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			previousLevels.clear();
			lastSpawnAnnounce.clear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// The first StatChanged per skill after login is the baseline, not a level-up
		Integer previous = previousLevels.put(event.getSkill(), event.getLevel());
		if (previous == null || event.getLevel() <= previous || !config.announceLevelUps())
		{
			return;
		}

		announce("Level up: " + event.getSkill().getName() + " is now " + event.getLevel() + ". The corgi approves.");
		soundEngine.play(Sound.LEVEL_UP);
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		Player player = event.getPlayer();
		if (player == client.getLocalPlayer() || !config.announceTargetSpawns() || !isTarget(player.getName()))
		{
			return;
		}

		String key = Text.standardize(player.getName());
		long now = System.currentTimeMillis();
		Long last = lastSpawnAnnounce.get(key);
		if (last != null && now - last < SPAWN_COOLDOWN_MS)
		{
			return;
		}
		lastSpawnAnnounce.put(key, now);

		announce(player.getName() + " has appeared!");
		soundEngine.play(Sound.TARGET_SPAWNED);
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!(event.getActor() instanceof Player))
		{
			return;
		}
		Player player = (Player) event.getActor();

		if (player == client.getLocalPlayer())
		{
			if (config.announceOwnDeath())
			{
				announce("You died. The corgi is disappointed.");
				soundEngine.play(Sound.OWN_DEATH);
			}
			return;
		}

		if (config.announceTargetDeaths() && isTarget(player.getName()))
		{
			announce(player.getName() + " has died!");
			soundEngine.play(Sound.TARGET_DEATH);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.PUBLICCHAT && config.targetChatSound() && isTarget(event.getName()))
		{
			soundEngine.play(Sound.TARGET_CHAT);
		}
	}

	private boolean isTarget(String name)
	{
		if (name == null)
		{
			return false;
		}
		String standardized = Text.standardize(name);
		for (String target : Text.fromCSV(config.targetPlayers()))
		{
			if (Text.standardize(target).equals(standardized))
			{
				return true;
			}
		}
		return false;
	}

	private void announce(String message)
	{
		if (config.chatMessagesEnabled() && client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
	}
}
