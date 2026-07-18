package com.skedpojkar.multiplayer;

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
 * Shared tic-tac-toe board over the RuneLite party service.
 *
 * Symbols are assigned deterministically: the party member with the lowest
 * member id is X, everyone else is O (designed for exactly two players).
 * X moves first and turns alternate. Every client applies the same rules to
 * the same server-ordered message stream, so boards stay consistent without
 * any extra coordination.
 */
@Slf4j
@Singleton
public class PartyTicTacToe
{
	private static final int[][] LINES = {
		{0, 1, 2}, {3, 4, 5}, {6, 7, 8},
		{0, 3, 6}, {1, 4, 7}, {2, 5, 8},
		{0, 4, 8}, {2, 4, 6},
	};

	private final PartyService partyService;
	private final String[] board = new String[9];

	/** "X", "O" or "draw" once the game has ended; null while playing. */
	private String winner;

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

	/** "X" if we have the lowest member id in the party, "O" otherwise; null when not in a party. */
	public String getLocalSymbol()
	{
		PartyMember local = partyService.getLocalMember();
		return local == null ? null : symbolFor(local.getMemberId());
	}

	public boolean isMyTurn()
	{
		String mine = getLocalSymbol();
		synchronized (board)
		{
			return mine != null && winner == null && mine.equals(nextTurn());
		}
	}

	/** One-line game status for the panel. */
	public String getGameStatus()
	{
		String mine = getLocalSymbol();
		if (mine == null)
		{
			return "Join a party to play.";
		}
		synchronized (board)
		{
			if ("draw".equals(winner))
			{
				return "Draw!";
			}
			if (winner != null)
			{
				return winner.equals(mine) ? "You win!" : "You lose!";
			}
			return "You are " + mine + (mine.equals(nextTurn())
				? " — your move." : " — waiting for " + nextTurn() + ".");
		}
	}

	public void sendMove(int cell)
	{
		// Local pre-check for responsiveness only; the authoritative check happens
		// when the message echoes back, identically on every client.
		if (!partyService.isInParty() || !isMyTurn())
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
		String symbol = symbolFor(message.getMemberId());

		synchronized (board)
		{
			if (PartyGameMessage.ACTION_RESET.equals(message.getAction()))
			{
				Arrays.fill(board, null);
				winner = null;
			}
			else if (PartyGameMessage.ACTION_MOVE.equals(message.getAction())
				&& winner == null
				&& message.getCell() >= 0 && message.getCell() < board.length
				&& board[message.getCell()] == null
				&& symbol.equals(nextTurn()))
			{
				board[message.getCell()] = symbol;
				checkGameEnd();
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
			winner = null;
		}
		notifyBoardChanged();
	}

	private String symbolFor(long memberId)
	{
		long lowest = partyService.getMembers().stream()
			.mapToLong(PartyMember::getMemberId)
			.min()
			.orElse(memberId);
		return memberId == lowest ? "X" : "O";
	}

	/** Whose move it is, derived from the board. Call with the board lock held. */
	private String nextTurn()
	{
		int x = 0;
		int o = 0;
		for (String cell : board)
		{
			if ("X".equals(cell))
			{
				x++;
			}
			else if ("O".equals(cell))
			{
				o++;
			}
		}
		return x == o ? "X" : "O";
	}

	/** Sets {@link #winner} if a line is complete or the board is full. Call with the board lock held. */
	private void checkGameEnd()
	{
		for (int[] line : LINES)
		{
			String first = board[line[0]];
			if (first != null && first.equals(board[line[1]]) && first.equals(board[line[2]]))
			{
				winner = first;
				return;
			}
		}
		for (String cell : board)
		{
			if (cell == null)
			{
				return;
			}
		}
		winner = "draw";
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
