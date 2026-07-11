package com.skedpojkar.sound;

import com.skedpojkar.SkedpojkarConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;

/**
 * Plays .wav files dropped by the user into ~/.runelite/skedpojkar-sounds.
 * No sounds ship with the plugin; a missing file simply logs and does nothing.
 * Playback goes through RuneLite's {@link AudioPlayer} (required for the Plugin Hub).
 */
@Slf4j
@Singleton
public class SoundEngine
{
	public static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "skedpojkar-sounds");

	@Inject
	private SkedpojkarConfig config;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private AudioPlayer audioPlayer;

	/**
	 * Creates the sound folder and drops a readme explaining which file names are used.
	 * Safe to call repeatedly; never throws.
	 */
	public void init()
	{
		try
		{
			if (!SOUND_DIR.exists() && !SOUND_DIR.mkdirs())
			{
				log.warn("Could not create sound directory {}", SOUND_DIR);
				return;
			}

			StringBuilder sb = new StringBuilder()
				.append("Drop .wav files with these exact names into this folder\n")
				.append("to give Skedpojkar its sounds:\n\n");
			for (Sound sound : Sound.values())
			{
				for (String fileName : sound.getFileNames())
				{
					sb.append(fileName).append('\n');
				}
			}
			Files.write(new File(SOUND_DIR, "README.txt").toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("Failed to initialise sound directory", e);
		}
	}

	public void play(Sound sound)
	{
		play(sound, 0);
	}

	/** Plays a sound after the given delay (milliseconds). */
	public void play(Sound sound, long delayMs)
	{
		if (!config.soundsEnabled() || config.soundVolume() <= 0)
		{
			return;
		}
		executor.schedule(() -> playSync(sound), delayMs, TimeUnit.MILLISECONDS);
	}

	private void playSync(Sound sound)
	{
		File file = new File(SOUND_DIR, sound.pickFileName());
		if (!file.exists())
		{
			log.debug("Sound file missing, skipping: {}", file);
			return;
		}

		// The 0-100 volume setting as a decibel gain: 100 -> 0 dB, 50 -> ~-6 dB, 25 -> ~-12 dB
		float gain = (float) (20.0 * Math.log10(config.soundVolume() / 100.0));

		// Deliberately broad catch: naming javax.sound exception types is itself
		// disallowed by the Plugin Hub's api scan
		try
		{
			audioPlayer.play(file, gain);
		}
		catch (Exception e)
		{
			log.warn("Failed to play {}. Only uncompressed PCM .wav files play — renaming an"
				+ " .mp3 to .wav is not enough; convert it (e.g. Audacity: Export as WAV,"
				+ " signed 16-bit PCM).", file.getName(), e);
		}
	}
}
