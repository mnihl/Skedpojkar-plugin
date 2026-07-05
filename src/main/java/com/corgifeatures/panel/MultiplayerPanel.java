package com.corgifeatures.panel;

import com.corgifeatures.multiplayer.PartyTicTacToe;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Party tic-tac-toe (prototype). Requires both players to run this plugin and
 * be in the same RuneLite party. Your moves show as X, party members' as O.
 */
public class MultiplayerPanel extends JPanel
{
	private final PartyTicTacToe game;
	private final JLabel status = new JLabel();
	private final JButton[] cells = new JButton[9];

	public MultiplayerPanel(PartyTicTacToe game)
	{
		this.game = game;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
		for (int i = 0; i < 9; i++)
		{
			final int cell = i;
			cells[i] = new JButton("");
			cells[i].setFont(cells[i].getFont().deriveFont(Font.BOLD, 20f));
			cells[i].addActionListener(e -> onCellClicked(cell));
			grid.add(cells[i]);
		}

		JButton resetButton = new JButton("Reset board");
		resetButton.addActionListener(e -> game.sendReset());

		JButton refreshButton = new JButton("Refresh party status");
		refreshButton.addActionListener(e -> refreshStatus());

		JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 4));
		buttons.add(resetButton);
		buttons.add(refreshButton);

		add(status, BorderLayout.NORTH);
		add(grid, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		game.setBoardListener(this::renderBoard);
		refreshStatus();
		renderBoard();
	}

	private void onCellClicked(int cell)
	{
		if (!game.isInParty())
		{
			refreshStatus();
			return;
		}
		// The board updates when the move echoes back from the party server,
		// so a short delay before the X appears is expected.
		game.sendMove(cell);
	}

	private void refreshStatus()
	{
		status.setText("<html>" + game.getPartyStatus() + "</html>");
	}

	private void renderBoard()
	{
		String[] board = game.getBoard();
		for (int i = 0; i < cells.length; i++)
		{
			cells[i].setText(board[i] == null ? "" : board[i]);
		}
	}
}
