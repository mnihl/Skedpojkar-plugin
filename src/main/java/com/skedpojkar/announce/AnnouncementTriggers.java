package com.skedpojkar.announce;

import com.skedpojkar.SkedpojkarConfig;
import com.skedpojkar.sound.Sound;
import com.skedpojkar.sound.SoundEngine;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Feature 1: reacts to in-game events with local chat messages and sounds.
 *
 * Triggers are keyed by event/message type only, never by player name —
 * player-targeted triggers are rejected by RuneLite (generic player
 * highlighting, see the Rejected-or-Rolled-Back-Features wiki page).
 */
@Slf4j
@Singleton
public class AnnouncementTriggers
{
	// PvP kill attribution: the API has no "you killed X" event, so we count a kill
	// as ours if we landed a hitsplat on the player within this window before death.
	private static final long KILL_ATTRIBUTION_WINDOW_MS = 5_000L;
	private static final long PVP_KILL_SOUND_DELAY_MS = 2_000L;
	private static final int PVP_ZERO_HIT_CHANCE_PERCENT = 30;

	// The Al Kharid toll gate sits on the x=3268 fence line, gate tiles at y 3227-3228.
	// TODO verify in-game; adjust if the sound fires at the wrong spot or not at all.
	private static final int GATE_X = 3268;
	private static final int GATE_MIN_Y = 3226;
	private static final int GATE_MAX_Y = 3229;

	// Wordings confirmed in-game 2026-07-11
	private static final String SEPULCHRE_FLOOR_5_MESSAGE = "You have completed Floor 5 of the Hallowed Sepulchre";
	private static final String GAUNTLET_MESSAGE = "Corrupted challenge duration";

	// Clan broadcast phrases. TODO verify against real broadcasts in-game; clans
	// only broadcast the event types enabled in their clan settings.
	private static final String CLAN_DEATH_PHRASE = "has been defeated by";
	private static final String CLAN_DEATH_PHRASE_2 = "has died";
	private static final String CLAN_KILL_PHRASE = "has defeated";
	private static final String CLAN_DROP_PHRASE = "received a drop";

	@Inject
	private Client client;

	@Inject
	private SkedpojkarConfig config;

	@Inject
	private SoundEngine soundEngine;

	private final Map<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private final Map<String, Long> lastPvpHit = new HashMap<>();
	private final Random random = new Random();

	private WorldPoint lastTickPosition;

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			previousLevels.clear();
			lastPvpHit.clear();
			lastTickPosition = null;
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

	/**
	 * Tracks our hits on other players (for kill attribution) and plays the
	 * PvP zero-hit sound on a 30% roll.
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!(event.getActor() instanceof Player) || event.getActor() == client.getLocalPlayer())
		{
			return;
		}
		Hitsplat hitsplat = event.getHitsplat();
		if (!hitsplat.isMine())
		{
			return;
		}

		Player player = (Player) event.getActor();
		if (player.getName() != null)
		{
			lastPvpHit.put(Text.standardize(player.getName()), System.currentTimeMillis());
		}

		if (hitsplat.getAmount() == 0 && config.pvpZeroHitSound()
			&& random.nextInt(100) < PVP_ZERO_HIT_CHANCE_PERCENT)
		{
			soundEngine.play(Sound.PVP_ZERO_HIT);
		}
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

		// PvP kill: we damaged this player recently, so count the kill as ours
		String key = player.getName() == null ? null : Text.standardize(player.getName());
		Long lastHit = key == null ? null : lastPvpHit.remove(key);
		if (config.pvpKillSound() && lastHit != null
			&& System.currentTimeMillis() - lastHit < KILL_ATTRIBUTION_WINDOW_MS)
		{
			soundEngine.play(Sound.PVP_KILL, PVP_KILL_SOUND_DELAY_MS);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = Text.removeTags(event.getMessage());

		switch (event.getType())
		{
			case GAMEMESSAGE:
				if (config.sepulchreSound() && message.startsWith(SEPULCHRE_FLOOR_5_MESSAGE))
				{
					soundEngine.play(Sound.SEPULCHRE_FLOOR_5);
				}
				else if (config.goodJobSound() && message.startsWith(GAUNTLET_MESSAGE))
				{
					soundEngine.play(Sound.GOOD_JOB);
				}
				break;

			// System broadcasts in the clan channel (kills, deaths, drops, ...).
			// Matched by broadcast type only — never by who is mentioned.
			case CLAN_MESSAGE:
				handleClanBroadcast(message);
				break;

			// Any member talking in the clan or friends channel
			case CLAN_CHAT:
			case FRIENDSCHAT:
				if (config.clanChatSound())
				{
					soundEngine.play(Sound.CLAN_CHAT);
				}
				break;

			default:
				break;
		}
	}

	private void handleClanBroadcast(String message)
	{
		// Deaths first: "X has been defeated by Y" must not match the kill phrase
		if (config.clanDeathSound()
			&& (message.contains(CLAN_DEATH_PHRASE) || message.contains(CLAN_DEATH_PHRASE_2)))
		{
			soundEngine.play(Sound.CLAN_DEATH);
		}
		else if (config.clanKillSound() && message.contains(CLAN_KILL_PHRASE))
		{
			soundEngine.play(Sound.CLAN_KILL);
		}
		else if (config.clanDropSound() && message.contains(CLAN_DROP_PHRASE))
		{
			soundEngine.play(Sound.CLAN_DROP);
		}
	}

	/**
	 * Detects passing the Al Kharid toll gate by watching for the player's position
	 * crossing the gate's fence line between ticks (running covers 2 tiles per tick,
	 * so checking "standing on the gate tile" would miss crossings).
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}
		WorldPoint current = local.getWorldLocation();
		WorldPoint previous = lastTickPosition;
		lastTickPosition = current;

		if (previous == null || !config.goodJobSound()
			|| current.getPlane() != 0 || previous.getPlane() != 0)
		{
			return;
		}

		boolean nearGate = current.getY() >= GATE_MIN_Y && current.getY() <= GATE_MAX_Y
			&& previous.getY() >= GATE_MIN_Y && previous.getY() <= GATE_MAX_Y;
		boolean crossed = (previous.getX() < GATE_X && current.getX() >= GATE_X)
			|| (previous.getX() >= GATE_X && current.getX() < GATE_X);
		if (nearGate && crossed)
		{
			soundEngine.play(Sound.GOOD_JOB);
		}
	}

	private void announce(String message)
	{
		if (config.chatMessagesEnabled() && client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
	}
}
