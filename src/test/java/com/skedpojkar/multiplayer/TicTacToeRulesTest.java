package com.skedpojkar.multiplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TicTacToeRulesTest
{
	/** Builds a board from 9 characters, row by row: X, O, or '.' for empty. */
	private static String[] board(String cells)
	{
		String[] board = new String[9];
		for (int i = 0; i < 9; i++)
		{
			char c = cells.charAt(i);
			board[i] = c == '.' ? null : String.valueOf(c);
		}
		return board;
	}

	@Test
	public void emptyBoardIsStillPlayingAndXStarts()
	{
		assertNull(TicTacToeRules.outcome(board(".........")));
		assertEquals("X", TicTacToeRules.nextTurn(board(".........")));
	}

	@Test
	public void turnsAlternate()
	{
		assertEquals("O", TicTacToeRules.nextTurn(board("X........")));
		assertEquals("X", TicTacToeRules.nextTurn(board("XO.......")));
		assertEquals("O", TicTacToeRules.nextTurn(board("XOX......")));
	}

	@Test
	public void everyLineWins()
	{
		String[] winningBoards = {
			"XXX......", "...XXX...", "......XXX", // rows
			"X..X..X..", ".X..X..X.", "..X..X..X", // columns
			"X...X...X", "..X.X.X..",              // diagonals
		};
		for (String cells : winningBoards)
		{
			assertEquals(cells, "X", TicTacToeRules.outcome(board(cells)));
			assertEquals(cells, "O", TicTacToeRules.outcome(board(cells.replace('X', 'O'))));
		}
	}

	@Test
	public void twoInARowIsNotAWin()
	{
		assertNull(TicTacToeRules.outcome(board("XX.OO....")));
	}

	@Test
	public void fullBoardWithNoLineIsADraw()
	{
		assertEquals(TicTacToeRules.DRAW, TicTacToeRules.outcome(board("XOXXOOOXX")));
	}

	@Test
	public void aWinOnTheLastMoveIsAWinNotADraw()
	{
		assertEquals("X", TicTacToeRules.outcome(board("XOXOXOOXX")));
	}

	@Test
	public void findsTheCellThatCompletesALine()
	{
		assertEquals(Integer.valueOf(2), TicTacToeRules.findLineMove(board("XX......."), "X"));
		assertEquals(Integer.valueOf(4), TicTacToeRules.findLineMove(board("O.......O"), "O"));
		assertEquals(Integer.valueOf(3), TicTacToeRules.findLineMove(board("X.....X.."), "X"));
	}

	@Test
	public void noLineMoveWhenTheThirdCellIsTakenOrThereIsOnlyOne()
	{
		assertNull(TicTacToeRules.findLineMove(board("XXO......"), "X"));
		assertNull(TicTacToeRules.findLineMove(board("X........"), "X"));
		assertNull(TicTacToeRules.findLineMove(board("XX......."), "O"));
	}
}
