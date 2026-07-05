package com.corgifeatures;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(CorgiFeaturesConfig.GROUP)
public interface CorgiFeaturesConfig extends Config
{
	String GROUP = "corgifeatures";

	@ConfigSection(
		name = "Announcements",
		description = "Chat messages and sounds triggered by in-game events",
		position = 0
	)
	String announcementsSection = "announcements";

	@ConfigSection(
		name = "Sounds",
		description = "Sound playback settings",
		position = 1
	)
	String soundsSection = "sounds";

	@ConfigItem(
		keyName = "targetPlayers",
		name = "Target players",
		description = "Comma-separated usernames to watch for (e.g. 'Zezima, Cow31337Killer'). Leave empty to disable player-specific triggers.",
		section = announcementsSection,
		position = 0
	)
	default String targetPlayers()
	{
		return "";
	}

	@ConfigItem(
		keyName = "chatMessagesEnabled",
		name = "Show chat messages",
		description = "Print announcement messages in your chatbox (only you can see them)",
		section = announcementsSection,
		position = 1
	)
	default boolean chatMessagesEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceLevelUps",
		name = "Announce your level-ups",
		description = "Trigger when one of your skills levels up",
		section = announcementsSection,
		position = 2
	)
	default boolean announceLevelUps()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceOwnDeath",
		name = "Announce your death",
		description = "Trigger when you die",
		section = announcementsSection,
		position = 3
	)
	default boolean announceOwnDeath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceTargetSpawns",
		name = "Announce target appearing",
		description = "Trigger when a target player comes into view",
		section = announcementsSection,
		position = 4
	)
	default boolean announceTargetSpawns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceTargetDeaths",
		name = "Announce target dying",
		description = "Trigger when a target player dies near you",
		section = announcementsSection,
		position = 5
	)
	default boolean announceTargetDeaths()
	{
		return true;
	}

	@ConfigItem(
		keyName = "targetChatSound",
		name = "Sound on target chat",
		description = "Play a sound when a target player says something in public chat",
		section = announcementsSection,
		position = 6
	)
	default boolean targetChatSound()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundsEnabled",
		name = "Enable sounds",
		description = "Play .wav sound files from the corgi-features-sounds folder in your .runelite directory",
		section = soundsSection,
		position = 0
	)
	default boolean soundsEnabled()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "soundVolume",
		name = "Sound volume",
		description = "Volume of played sounds (0-100)",
		section = soundsSection,
		position = 1
	)
	default int soundVolume()
	{
		return 100;
	}
}
