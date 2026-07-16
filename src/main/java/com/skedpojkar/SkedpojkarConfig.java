package com.skedpojkar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(SkedpojkarConfig.GROUP)
public interface SkedpojkarConfig extends Config
{
	String GROUP = "skedpojkar";

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
		keyName = "clanKillSound",
		name = "Sound on clan PvP kill broadcast",
		description = "Play a sound when your clan broadcasts that a member defeated another player",
		section = announcementsSection,
		position = 4
	)
	default boolean clanKillSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanDeathSound",
		name = "Sound on clan death broadcast",
		description = "Play a sound when your clan broadcasts that a member died",
		section = announcementsSection,
		position = 5
	)
	default boolean clanDeathSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanDropSound",
		name = "Sound on clan drop broadcast",
		description = "Play a sound when your clan broadcasts that a member received a drop",
		section = announcementsSection,
		position = 6
	)
	default boolean clanDropSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanChatSound",
		name = "Sound on clan/friends chat",
		description = "Play a sound whenever anyone talks in your clan or friends channel (can be noisy)",
		section = announcementsSection,
		position = 7
	)
	default boolean clanChatSound()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pvpZeroHitSound",
		name = "Sound on PvP zero hit",
		description = "30% chance to play a sound when you hit a 0 on another player",
		section = announcementsSection,
		position = 8
	)
	default boolean pvpZeroHitSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pvpKillSound",
		name = "Sound on PvP kill",
		description = "Play a sound (2s delayed) when a player you recently damaged dies",
		section = announcementsSection,
		position = 9
	)
	default boolean pvpKillSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sepulchreSound",
		name = "Sound on Sepulchre floor V",
		description = "Play a sound when you complete the highest floor of the Hallowed Sepulchre",
		section = announcementsSection,
		position = 10
	)
	default boolean sepulchreSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "goodJobSound",
		name = "Sound on gate/Gauntlet",
		description = "Play a sound when you pass the Al Kharid gate or complete the Corrupted Gauntlet",
		section = announcementsSection,
		position = 11
	)
	default boolean goodJobSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "princeAliGateMessage",
		name = "Prince Ali gate message",
		description = "Chat message when you pass the Al Kharid gate having completed Prince Ali Rescue",
		section = announcementsSection,
		position = 12
	)
	default boolean princeAliGateMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "houseAmenitiesCheck",
		name = "House amenities check",
		description = "Chat message when entering a player-owned house that lacks an ornate pool or ornate jewellery box",
		section = announcementsSection,
		position = 14
	)
	default boolean houseAmenitiesCheck()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankValueWarning",
		name = "Low bank value warning",
		description = "Chat message (once per session) when your opened bank is worth less than the threshold",
		section = announcementsSection,
		position = 15
	)
	default boolean bankValueWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankValueThreshold",
		name = "Bank value threshold",
		description = "The bank value (in gp, GE prices) below which the warning shows",
		section = announcementsSection,
		position = 16
	)
	default int bankValueThreshold()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "soundsEnabled",
		name = "Enable sounds",
		description = "Play .wav sound files from the skedpojkar-sounds folder in your .runelite directory",
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
		return 25;
	}
}
