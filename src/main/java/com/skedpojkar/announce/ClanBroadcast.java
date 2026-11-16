package com.skedpojkar.announce;

/**
 * The kinds of clan system broadcast the plugin reacts to. Matched by the
 * wording of the broadcast only — never by who is mentioned in it.
 */
public enum ClanBroadcast
{
	DEATH,
	KILL,
	DROP,
	OTHER;

	// Clan broadcast phrases. TODO verify against real broadcasts in-game; clans
	// only broadcast the event types enabled in their clan settings.
	private static final String DEATH_PHRASE = "has been defeated by";
	private static final String DEATH_PHRASE_2 = "has died";
	private static final String KILL_PHRASE = "has defeated";
	private static final String DROP_PHRASE = "received a drop";

	/** Works out which kind of broadcast a clan system message is. */
	public static ClanBroadcast classify(String message)
	{
		// Deaths first: "X has been defeated by Y" must not count as a kill
		if (message.contains(DEATH_PHRASE) || message.contains(DEATH_PHRASE_2))
		{
			return DEATH;
		}
		if (message.contains(KILL_PHRASE))
		{
			return KILL;
		}
		if (message.contains(DROP_PHRASE))
		{
			return DROP;
		}
		return OTHER;
	}
}
