package com.corgifeatures.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Shared look for the tic-tac-toe boards: square cells that stand out from the
 * side panel, and colored symbols (X cyan, O orange).
 */
final class TttUi
{
	static final Color X_COLOR = new Color(0, 200, 255);
	static final Color O_COLOR = ColorScheme.BRAND_ORANGE;

	private TttUi()
	{
	}

	static JButton createCell()
	{
		JButton cell = new JButton("");
		cell.setPreferredSize(new Dimension(58, 58));
		cell.setFont(cell.getFont().deriveFont(Font.BOLD, 26f));
		cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		cell.setFocusPainted(false);
		cell.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));
		return cell;
	}

	/** Sets a cell's symbol ("X", "O" or null to clear) with its color. */
	static void setCell(JButton cell, String symbol)
	{
		cell.setText(symbol == null ? "" : symbol);
		cell.setForeground("O".equals(symbol) ? O_COLOR : X_COLOR);
	}

	/** Wraps the 3x3 grid so it keeps its square size instead of stretching to fill the panel. */
	static JPanel center(JPanel grid)
	{
		JPanel wrapper = new JPanel(new GridBagLayout());
		wrapper.setOpaque(false);
		wrapper.add(grid);
		return wrapper;
	}
}
