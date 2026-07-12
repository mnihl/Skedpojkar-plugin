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
 * Plays the sounds bundled with the plugin (this package's .wav resources).
 * A file with the same name in ~/.runelite/skedpojkar-sounds takes precedence,
 * so users can override any sound or add ones the plugin doesn't ship.
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
				.append("Drop .wav files (uncompressed PCM) with these exact names into this\n")
				.append("folder to override Skedpojkar's built-in sounds, or to add sounds\n")
				.append("for triggers that don't ship with one:\n\n");
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
		String fileName = sound.pickFileName();

		// The 0-100 volume setting as a decibel gain: 100 -> 0 dB, 50 -> ~-6 dB, 25 -> ~-12 dB
		float gain = (float) (20.0 * Math.log10(config.soundVolume() / 100.0));

		// Deliberately broad catch: naming javax.sound exception types is itself
		// disallowed by the Plugin Hub's api scan
		try
		{
			File file = new File(SOUND_DIR, fileName);
			if (file.exists())
			{
				// A user-provided file overrides the bundled sound
				audioPlayer.play(file, gain);
			}
			else if (SoundEngine.class.getResource(fileName) != null)
			{
				// Bundled default shipped inside the plugin jar
				audioPlayer.play(SoundEngine.class, fileName, gain);
			}
			else
			{
				log.debug("No user file or bundled sound for {}, skipping", fileName);
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to play {}. Only uncompressed PCM .wav files play — renaming an"
				+ " .mp3 to .wav is not enough; convert it (e.g. Audacity: Export as WAV,"
				+ " signed 16-bit PCM).", fileName, e);
		}
	}
}
