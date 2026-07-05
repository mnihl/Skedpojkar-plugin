package com.corgifeatures.multiplayer;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;

/**
 * Prototype shared tic-tac-toe board over the RuneLite party service.
 *
 * Deliberately simple for now: every move (including your own) is applied when it
 * comes back from the party server, your moves render as X and everyone else's as O,
 * and there is no turn enforcement or win detection yet. Works with exactly two
 * party members; degrades to scribbles with more.
 */
@Slf4j
@Singleton
public class PartyTicTacToe
{
	private final PartyService partyService;
	private final String[] board = new String[9];

	/** Invoked on the Swing thread whenever the board changes. */
	@Setter
	private Runnable boardListener;

	@Inject
	public PartyTicTacToe(PartyService partyService)
	{
		this.partyService = partyService;
	}

	public boolean isInParty()
	{
		return partyService.isInParty();
	}

	public String getPartyStatus()
	{
		if (!partyService.isInParty())
		{
			return "Not in a party. Join one via the built-in Party plugin (same passphrase as your friend), then hit Refresh.";
		}
		String members = partyService.getMembers().stream()
			.map(PartyMember::getDisplayName)
			.collect(Collectors.joining(", "));
		return "In a party with: " + members;
	}

	public String[] getBoard()
	{
		synchronized (board)
		{
			return board.clone();
		}
	}

	public void sendMove(int cell)
	{
		if (!partyService.isInParty())
		{
			return;
		}
		partyService.send(new PartyGameMessage(PartyGameMessage.ACTION_MOVE, cell));
	}

	public void sendReset()
	{
		if (!partyService.isInParty())
		{
			return;
		}
		partyService.send(new PartyGameMessage(PartyGameMessage.ACTION_RESET, -1));
	}

	@Subscribe
	public void onPartyGameMessage(PartyGameMessage message)
	{
		// The party server echoes our own messages back, so both players
		// (sender included) apply moves here and stay in sync.
		PartyMember local = partyService.getLocalMember();
		boolean self = local != null && local.getMemberId() == message.getMemberId();

		synchronized (board)
		{
			if (PartyGameMessage.ACTION_RESET.equals(message.getAction()))
			{
				Arrays.fill(board, null);
			}
			else if (PartyGameMessage.ACTION_MOVE.equals(message.getAction())
				&& message.getCell() >= 0 && message.getCell() < board.length
				&& board[message.getCell()] == null)
			{
				board[message.getCell()] = self ? "X" : "O";
			}
		}
		notifyBoardChanged();
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		synchronized (board)
		{
			Arrays.fill(board, null);
		}
		notifyBoardChanged();
	}

	private void notifyBoardChanged()
	{
		Runnable listener = boardListener;
		if (listener != null)
		{
			SwingUtilities.invokeLater(listener);
		}
	}
}
