package com.corgifeatures.sound;

import com.corgifeatures.CorgiFeaturesConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Plays .wav files dropped by the user into ~/.runelite/corgi-features-sounds.
 * No sounds ship with the plugin yet; a missing file simply logs and does nothing.
 */
@Slf4j
@Singleton
public class SoundEngine
{
	public static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "corgi-features-sounds");

	@Inject
	private CorgiFeaturesConfig config;

	@Inject
	private ScheduledExecutorService executor;

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
				.append("to give Corgi's Various Features its sounds:\n\n");
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
		if (!config.soundsEnabled() || config.soundVolume() <= 0)
		{
			return;
		}
		executor.execute(() -> playSync(sound));
	}

	private void playSync(Sound sound)
	{
		File file = new File(SOUND_DIR, sound.pickFileName());
		if (!file.exists())
		{
			log.debug("Sound file missing, skipping: {}", file);
			return;
		}

		try (AudioInputStream stream = AudioSystem.getAudioInputStream(file))
		{
			Clip clip = AudioSystem.getClip();
			clip.open(stream);
			setVolume(clip);
			clip.addLineListener(event ->
			{
				if (event.getType() == LineEvent.Type.STOP)
				{
					clip.close();
				}
			});
			clip.start();
		}
		catch (UnsupportedAudioFileException e)
		{
			log.warn("{} is not a playable .wav file. Java only plays uncompressed PCM wavs — "
				+ "convert it (e.g. Audacity: Export as WAV, signed 16-bit PCM). "
				+ "Renaming an .mp3 to .wav is not enough.", file.getName());
		}
		catch (Exception e)
		{
			log.warn("Failed to play sound {}", sound, e);
		}
	}

	private void setVolume(Clip clip)
	{
		try
		{
			FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float db = (float) (20.0 * Math.log10(config.soundVolume() / 100.0));
			gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
		}
		catch (IllegalArgumentException e)
		{
			log.debug("Volume control not supported for this clip", e);
		}
	}
}
