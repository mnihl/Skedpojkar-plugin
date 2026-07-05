package com.corgifeatures.sound;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Every sound the plugin can play, mapped to the .wav file name expected in
 * the corgi-features-sounds folder. Missing files are skipped gracefully.
 */
@Getter
@RequiredArgsConstructor
public enum Sound
{
	LEVEL_UP("level_up.wav"),
	OWN_DEATH("own_death.wav"),
	TARGET_SPAWNED("friend_spawned.wav"),
	TARGET_DEATH("friend_death.wav"),
	TARGET_CHAT("friend_chat.wav");

	private final String fileName;
}
