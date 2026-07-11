package com.corgifeatures.panel;

import com.corgifeatures.multiplayer.PartyTicTacToe;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Party tic-tac-toe. Requires both players to run this plugin and be in the
 * same RuneLite party. The member with the lowest party id is X and moves
 * first; turns alternate and wins/draws are detected.
 */
public class MultiplayerPanel extends JPanel
{
	private final PartyTicTacToe game;
	private final JLabel partyStatus = new JLabel();
	private final JLabel gameStatus = new JLabel();
	private final JButton[] cells = new JButton[9];

	public MultiplayerPanel(PartyTicTacToe game)
	{
		this.game = game;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		gameStatus.setFont(gameStatus.getFont().deriveFont(Font.BOLD));

		JPanel statusBox = new JPanel();
		statusBox.setLayout(new BoxLayout(statusBox, BoxLayout.Y_AXIS));
		statusBox.setOpaque(false);
		statusBox.add(partyStatus);
		statusBox.add(gameStatus);

		JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
		grid.setOpaque(false);
		for (int i = 0; i < 9; i++)
		{
			final int cell = i;
			cells[i] = TttUi.createCell();
			cells[i].addActionListener(e -> onCellClicked(cell));
			grid.add(cells[i]);
		}

		JButton resetButton = new JButton("Reset board");
		resetButton.addActionListener(e -> game.sendReset());

		JButton refreshButton = new JButton("Refresh party status");
		refreshButton.addActionListener(e -> refresh());

		JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 4));
		buttons.setOpaque(false);
		buttons.add(resetButton);
		buttons.add(refreshButton);

		add(statusBox, BorderLayout.NORTH);
		add(TttUi.center(grid), BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		game.setBoardListener(this::refresh);
		refresh();
	}

	private void onCellClicked(int cell)
	{
		// Ignored by the game when it's not our turn or we're not in a party.
		// The board updates when the move echoes back from the party server,
		// so a short delay before the symbol appears is expected.
		game.sendMove(cell);
		refresh();
	}

	private void refresh()
	{
		partyStatus.setText("<html>" + game.getPartyStatus() + "</html>");
		gameStatus.setText(game.getGameStatus());

		String[] board = game.getBoard();
		for (int i = 0; i < cells.length; i++)
		{
			TttUi.setCell(cells[i], board[i]);
		}
	}
}
