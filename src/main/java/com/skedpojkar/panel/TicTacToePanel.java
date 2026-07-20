package com.skedpojkar.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Single-player tic-tac-toe against a simple AI (win > block > center > random).
 * You are X, the AI is O.
 */
public class TicTacToePanel extends JPanel
{
	private final com.skedpojkar.achievements.AchievementManager achievements;

	private static final int[][] LINES = {
		{0, 1, 2}, {3, 4, 5}, {6, 7, 8},
		{0, 3, 6}, {1, 4, 7}, {2, 5, 8},
		{0, 4, 8}, {2, 4, 6},
	};

	private final JButton[] cells = new JButton[9];
	private final String[] board = new String[9];
	private final JLabel status = new JLabel("Your move. You are X.", SwingConstants.CENTER);
	private final Random random = new Random();

	private boolean gameOver;

	public TicTacToePanel(com.skedpojkar.achievements.AchievementManager achievements)
	{
		this.achievements = achievements;
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
		grid.setOpaque(false);
		for (int i = 0; i < 9; i++)
		{
			final int cell = i;
			cells[i] = TttUi.createCell();
			cells[i].addActionListener(e -> onPlayerMove(cell));
			grid.add(cells[i]);
		}

		JButton reset = new JButton("New game");
		reset.addActionListener(e -> resetGame());

		add(status, BorderLayout.NORTH);
		add(TttUi.center(grid), BorderLayout.CENTER);
		add(reset, BorderLayout.SOUTH);
	}

	private void onPlayerMove(int cell)
	{
		if (gameOver || board[cell] != null)
		{
			return;
		}
		place(cell, "X");
		if (checkGameEnd())
		{
			return;
		}

		place(pickAiMove(), "O");
		checkGameEnd();
	}

	private void place(int cell, String symbol)
	{
		board[cell] = symbol;
		TttUi.setCell(cells[cell], symbol);
	}

	private int pickAiMove()
	{
		Integer winning = findLineMove("O");
		if (winning != null)
		{
			return winning;
		}
		Integer blocking = findLineMove("X");
		if (blocking != null)
		{
			return blocking;
		}
		if (board[4] == null)
		{
			return 4;
		}
		List<Integer> free = new ArrayList<>();
		for (int i = 0; i < 9; i++)
		{
			if (board[i] == null)
			{
				free.add(i);
			}
		}
		return free.get(random.nextInt(free.size()));
	}

	/** Finds the empty cell completing a line that already has two of {@code symbol}, if any. */
	private Integer findLineMove(String symbol)
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

	private boolean checkGameEnd()
	{
		for (int[] line : LINES)
		{
			String first = board[line[0]];
			if (first != null && first.equals(board[line[1]]) && first.equals(board[line[2]]))
			{
				gameOver = true;
				if (first.equals("X"))
				{
					achievements.unlock(com.skedpojkar.achievements.Achievement.TACTICIAN);
				}
				status.setText(first.equals("X") ? "You win!" : "The AI wins!");
				return true;
			}
		}
		for (String cell : board)
		{
			if (cell == null)
			{
				return false;
			}
		}
		gameOver = true;
		status.setText("Draw!");
		return true;
	}

	private void resetGame()
	{
		gameOver = false;
		for (int i = 0; i < 9; i++)
		{
			board[i] = null;
			TttUi.setCell(cells[i], null);
		}
		status.setText("Your move. You are X.");
	}
}
