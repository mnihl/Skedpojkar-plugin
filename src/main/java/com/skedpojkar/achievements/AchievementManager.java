package com.skedpojkar.achievements;

import com.skedpojkar.SkedpojkarConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/**
 * Plugin-wide achievement tracking, persisted per character. Any feature can
 * call {@link #unlock} or {@link #count}; unlocks announce in the chatbox
 * (no sounds). All storage goes through the RS profile so achievements follow
 * the character, and writes are refused while no character is resolvable.
 */
@Slf4j
@Singleton
public class AchievementManager
{
	private static final String UNLOCKED_KEY = "achievements";
	private static final String COUNTER_PREFIX = "achCounter_";

	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SkedpojkarConfig config;

	/** The unlocked achievement ids for the current character. */
	public Set<String> getUnlocked()
	{
		String stored = configManager.getRSProfileConfiguration(SkedpojkarConfig.GROUP, UNLOCKED_KEY);
		Set<String> set = new HashSet<>();
		if (stored != null && !stored.isEmpty())
		{
			Collections.addAll(set, stored.split(","));
		}
		return set;
	}

	public boolean isUnlocked(Achievement achievement)
	{
		return getUnlocked().contains(achievement.name());
	}

	/** Unlocks an achievement (idempotent) and announces it in the chatbox. */
	public void unlock(Achievement achievement)
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}
		Set<String> unlocked = getUnlocked();
		if (!unlocked.add(achievement.name()))
		{
			return;
		}
		configManager.setRSProfileConfiguration(SkedpojkarConfig.GROUP, UNLOCKED_KEY,
			String.join(",", unlocked));
		log.debug("Achievement unlocked: {}", achievement.name());

		if (!config.chatMessagesEnabled())
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Achievement unlocked: " + achievement.getDisplayName() + "!", null);
			}
		});
	}

	/**
	 * Increments this achievement's per-character counter and unlocks it once
	 * the counter reaches {@code needed}.
	 */
	public void count(Achievement achievement, int needed)
	{
		if (configManager.getRSProfileKey() == null || isUnlocked(achievement))
		{
			return;
		}
		String key = COUNTER_PREFIX + achievement.name();
		int value = 0;
		try
		{
			String stored = configManager.getRSProfileConfiguration(SkedpojkarConfig.GROUP, key);
			value = stored == null ? 0 : Integer.parseInt(stored);
		}
		catch (NumberFormatException e)
		{
			// treat as 0
		}
		value++;
		configManager.setRSProfileConfiguration(SkedpojkarConfig.GROUP, key, value);
		if (value >= needed)
		{
			unlock(achievement);
		}
	}
}
