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

	@Inject
	private Client client;

	@Inject
	private SkedpojkarConfig config;

	@Inject
	private SoundEngine soundEngine;

	private final Map<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private final Map<String, Long> lastSpawnAnnounce = new HashMap<>();
	private final Map<String, Long> lastPvpHit = new HashMap<>();
	private final Random random = new Random();

	private WorldPoint lastTickPosition;

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			previousLevels.clear();
			lastSpawnAnnounce.clear();
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

		if (event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			String message = Text.removeTags(event.getMessage());
			if (config.sepulchreSound() && message.startsWith(SEPULCHRE_FLOOR_5_MESSAGE))
			{
				soundEngine.play(Sound.SEPULCHRE_FLOOR_5);
			}
			else if (config.goodJobSound() && message.startsWith(GAUNTLET_MESSAGE))
			{
				soundEngine.play(Sound.GOOD_JOB);
			}
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
