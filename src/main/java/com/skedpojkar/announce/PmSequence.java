package com.skedpojkar.announce;

/**
 * Tracks progress through a secret sequence of private messages: each
 * incoming PM must exactly match the next phrase (case-insensitive, ignoring
 * surrounding spaces). A wrong message resets progress — or counts as a fresh
 * start if it matches the first phrase. Matches message content only,
 * regardless of who sent it.
 */
public class PmSequence
{
	private final String[] phrases;
	private int progress;

	public PmSequence(String... phrases)
	{
		this.phrases = phrases.clone();
	}

	/** Feeds one incoming PM. Returns true when that message completes the sequence. */
	public boolean advance(String message)
	{
		String msg = message.trim();
		if (msg.equalsIgnoreCase(phrases[progress]))
		{
			progress++;
		}
		else
		{
			progress = msg.equalsIgnoreCase(phrases[0]) ? 1 : 0;
		}

		if (progress >= phrases.length)
		{
			progress = 0;
			return true;
		}
		return false;
	}

	public void reset()
	{
		progress = 0;
	}
}
