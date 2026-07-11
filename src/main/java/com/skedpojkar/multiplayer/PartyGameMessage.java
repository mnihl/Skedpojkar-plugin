package com.skedpojkar.multiplayer;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Message relayed between party members over RuneLite's party websocket.
 * Serialized with gson by the party service, so keep fields simple.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PartyGameMessage extends PartyMemberMessage
{
	public static final String ACTION_MOVE = "move";
	public static final String ACTION_RESET = "reset";

	String action;
	int cell;
}
