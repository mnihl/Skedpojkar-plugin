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
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
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

	// The secret PM sequence: these phrases must arrive in this order as private
	// messages (each its own PM, exact match, case-insensitive) to trigger the
	// reaction. Hardcoded on purpose. Placeholder phrases — replace with the real ones.
	private static final String[] PM_SEQUENCE_PHRASES = {
		"yag",
		"si",
		"pilif",
	};

	// Player-owned-house amenity check. Object names verified in-game 2026-07-18:
	// the exit is named just "Portal", and a POH is full of "... space" build
	// hotspots — requiring both is a solid house fingerprint that other
	// instanced content won't match.
	private static final String POH_PORTAL_NAME = "Portal";
	private static final String POH_HOTSPOT_SUFFIX = " space";
	private static final String POH_ORNATE_POOL_PREFIX = "Ornate pool";
	private static final String POH_ORNATE_JEWELLERY_BOX_NAME = "Ornate jewellery box";
	// Ticks to wait after a region load before judging the house contents
	private static final int POH_SETTLE_TICKS = 3;

	@Inject
	private Client client;

	@Inject
	private SkedpojkarConfig config;

	@Inject
	private SoundEngine soundEngine;

	@Inject
	private ItemManager itemManager;

	private final Map<Skill, Integer> previousLevels = new EnumMap<>(Skill.class);
	private final Map<String, Long> lastPvpHit = new HashMap<>();
	private final Random random = new Random();

	private WorldPoint lastTickPosition;
	private int pmSequenceProgress;
	private boolean bankWarnedThisSession;

	// POH scan state, reset on every region load
	private boolean houseCheckPending;
	private int houseSettleTicks;
	private boolean sawExitPortal;
	private boolean sawBuildHotspot;
	private boolean sawOrnatePool;
	private boolean sawOrnateJewelleryBox;
	private final java.util.Set<String> scannedObjectNames = new java.util.HashSet<>();

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			previousLevels.clear();
			lastPvpHit.clear();
			lastTickPosition = null;
			pmSequenceProgress = 0;
			bankWarnedThisSession = false;
		}
		else if (event.getGameState() == GameState.LOADING)
		{
			// Every region load could be a house teleport; collect objects and judge later
			houseCheckPending = true;
			houseSettleTicks = 0;
			sawExitPortal = false;
			sawBuildHotspot = false;
			sawOrnatePool = false;
			sawOrnateJewelleryBox = false;
			scannedObjectNames.clear();
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

		announce("Level up: " + event.getSkill().getName() + " is now " + event.getLevel() + ". Gamer.");
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
				announce("You died.");
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
			// SPAM is the "Game: Filtered" message type — completion/duration
			// messages (Sepulchre, Gauntlet) often arrive as SPAM, not GAMEMESSAGE
			case GAMEMESSAGE:
			case SPAM:
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

			// Incoming private messages: secret-sequence detection.
			// Matches message content only, regardless of who sent it.
			case PRIVATECHAT:
				handlePrivateMessage(message);
				break;

			default:
				break;
		}
	}

	/**
	 * Advances through the hardcoded secret phrase sequence: each incoming PM
	 * must exactly match the next phrase (case-insensitive). A wrong message
	 * resets progress (or counts as a fresh start if it matches the first
	 * phrase). Completing the sequence triggers the reaction.
	 */
	private void handlePrivateMessage(String message)
	{
		String msg = message.trim();
		if (msg.equalsIgnoreCase(PM_SEQUENCE_PHRASES[pmSequenceProgress]))
		{
			pmSequenceProgress++;
		}
		else
		{
			pmSequenceProgress = msg.equalsIgnoreCase(PM_SEQUENCE_PHRASES[0]) ? 1 : 0;
		}

		if (pmSequenceProgress >= PM_SEQUENCE_PHRASES.length)
		{
			pmSequenceProgress = 0;
			announce("The secret sequence has been spoken.");
			soundEngine.play(Sound.PM_SEQUENCE);
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

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}

		checkGateCrossing(local);
		checkHouseAmenities();
	}

	/**
	 * Detects passing the Al Kharid toll gate by watching for the player's position
	 * crossing the gate's fence line between ticks (running covers 2 tiles per tick,
	 * so checking "standing on the gate tile" would miss crossings).
	 */
	private void checkGateCrossing(Player local)
	{
		WorldPoint current = local.getWorldLocation();
		WorldPoint previous = lastTickPosition;
		lastTickPosition = current;

		if (previous == null || current.getPlane() != 0 || previous.getPlane() != 0)
		{
			return;
		}

		boolean nearGate = current.getY() >= GATE_MIN_Y && current.getY() <= GATE_MAX_Y
			&& previous.getY() >= GATE_MIN_Y && previous.getY() <= GATE_MAX_Y;
		boolean crossed = (previous.getX() < GATE_X && current.getX() >= GATE_X)
			|| (previous.getX() >= GATE_X && current.getX() < GATE_X);
		if (!nearGate || !crossed)
		{
			return;
		}

		if (config.goodJobSound())
		{
			soundEngine.play(Sound.GOOD_JOB);
		}
		if (config.princeAliGateMessage()
			&& Quest.PRINCE_ALI_RESCUE.getState(client) == QuestState.FINISHED)
		{
			announce("Hey, you've done Prince Ali Rescue, wow!");
		}
	}

	/**
	 * A few ticks after a region load, judge whether we're in a player-owned house
	 * (spotted its exit portal) that lacks the best pool and/or jewellery box.
	 */
	private void checkHouseAmenities()
	{
		if (!houseCheckPending || ++houseSettleTicks < POH_SETTLE_TICKS)
		{
			return;
		}
		houseCheckPending = false;

		if (!scannedObjectNames.isEmpty())
		{
			log.debug("Instance scan: portal={} hotspots={} pool={} box={} objects={}",
				sawExitPortal, sawBuildHotspot, sawOrnatePool, sawOrnateJewelleryBox, scannedObjectNames);
		}

		if (!config.houseAmenitiesCheck() || !sawExitPortal || !sawBuildHotspot)
		{
			return;
		}
		if (!sawOrnatePool || !sawOrnateJewelleryBox)
		{
			// Placeholder message — replace with the real joke later
			announce("Brokie house.");
		}
	}

	/**
	 * Collects notable objects while a (possible) house is loading. Only runs in
	 * instanced regions, which is where player-owned houses live.
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!houseCheckPending || !client.isInInstancedRegion())
		{
			return;
		}

		ObjectComposition def = client.getObjectDefinition(event.getGameObject().getId());
		if (def != null && def.getImpostorIds() != null)
		{
			// Built furniture (pools, jewellery boxes...) are varbit-transformed
			// objects: the base definition is a nameless hotspot, the impostor is
			// what's actually built
			def = def.getImpostor();
		}
		String name = def == null ? null : def.getName();
		if (name == null || "null".equals(name))
		{
			return;
		}

		scannedObjectNames.add(name);

		if (POH_PORTAL_NAME.equalsIgnoreCase(name))
		{
			sawExitPortal = true;
		}
		else if (name.endsWith(POH_HOTSPOT_SUFFIX))
		{
			sawBuildHotspot = true;
		}
		else if (name.startsWith(POH_ORNATE_POOL_PREFIX))
		{
			sawOrnatePool = true;
		}
		else if (POH_ORNATE_JEWELLERY_BOX_NAME.equalsIgnoreCase(name))
		{
			sawOrnateJewelleryBox = true;
		}
	}

	/**
	 * Warns (once per session) when the bank is opened and its rough value —
	 * GE prices via ItemManager — is under the configured threshold.
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK.getId()
			|| !config.bankValueWarning() || bankWarnedThisSession)
		{
			return;
		}

		ItemContainer bank = event.getItemContainer();
		if (bank == null)
		{
			return;
		}

		long total = 0;
		for (Item item : bank.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				total += (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
			}
		}

		if (total < config.bankValueThreshold())
		{
			bankWarnedThisSession = true;
			// Placeholder message — replace with the real joke later
			announce("Pfft. Bank value under " + config.bankValueThreshold()
				+ " Brokie.");
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
