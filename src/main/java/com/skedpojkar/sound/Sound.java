package com.skedpojkar.sound;

import java.util.Random;

/**
 * Every sound the plugin can play, mapped to the .wav file name(s) expected in
 * the skedpojkar-sounds folder. Entries with multiple file names play one
 * at random. Missing files are skipped gracefully.
 */
public enum Sound
{
	LEVEL_UP("level_up.wav"),
	OWN_DEATH("own_death_1.wav", "own_death_2.wav"),
	CLAN_KILL("clan_kill.wav"),
	CLAN_DEATH("clan_death.wav"),
	CLAN_DROP("clan_drop.wav"),
	CLAN_CHAT("clan_chat.wav"),
	PVP_ZERO_HIT("pvp_zero_hit.wav"),
	PVP_KILL("pvp_kill.wav"),
	SEPULCHRE_FLOOR_5("sepulchre_floor_5.wav"),
	GOOD_JOB("good_job.wav"),
	PM_SEQUENCE("pm_sequence.wav");

	private static final Random RANDOM = new Random();

	private final String[] fileNames;

	Sound(String... fileNames)
	{
		this.fileNames = fileNames;
	}

	/** All file names this sound can play, e.g. for the folder README. */
	public String[] getFileNames()
	{
		return fileNames.clone();
	}

	/** The file to play right now — a random pick when there are multiple. */
	public String pickFileName()
	{
		return fileNames[RANDOM.nextInt(fileNames.length)];
	}
}
