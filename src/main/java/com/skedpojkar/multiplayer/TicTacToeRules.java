package com.skedpojkar.multiplayer;

/**
 * The rules of tic-tac-toe, shared by the single-player tab and the party
 * game. A board is 9 cells, row by row, each "X", "O" or null (empty).
 * Pure functions with no UI, so they can be unit-tested.
 */
public final class TicTacToeRules
{
	public static final String X = "X";
	public static final String O = "O";
	public static final String DRAW = "draw";

	private static final int[][] LINES = {
		{0, 1, 2}, {3, 4, 5}, {6, 7, 8},
		{0, 3, 6}, {1, 4, 7}, {2, 5, 8},
		{0, 4, 8}, {2, 4, 6},
	};

	private TicTacToeRules()
	{
	}

	/** "X" or "O" if someone has three in a row, "draw" if the board is full, null while still playing. */
	public static String outcome(String[] board)
	{
		for (int[] line : LINES)
		{
			String first = board[line[0]];
			if (first != null && first.equals(board[line[1]]) && first.equals(board[line[2]]))
			{
				return first;
			}
		}
		for (String cell : board)
		{
			if (cell == null)
			{
				return null;
			}
		}
		return DRAW;
	}

	/** Whose move it is, derived from the board: X moves first and turns alternate. */
	public static String nextTurn(String[] board)
	{
		int x = 0;
		int o = 0;
		for (String cell : board)
		{
			if (X.equals(cell))
			{
				x++;
			}
			else if (O.equals(cell))
			{
				o++;
			}
		}
		return x == o ? X : O;
	}

	/** The empty cell completing a line that already has two of {@code symbol}, or null if there is none. */
	public static Integer findLineMove(String[] board, String symbol)
	{
		for (int[] line : LINES)
		{
			int count = 0;
			int empty = -1;
			for (int cell : line)
			{
				if (symbol.equals(board[cell]))
				{
					count++;
				}
				else if (board[cell] == null)
				{
					empty = cell;
				}
			}
			if (count == 2 && empty != -1)
			{
				return empty;
			}
		}
		return null;
	}
}
